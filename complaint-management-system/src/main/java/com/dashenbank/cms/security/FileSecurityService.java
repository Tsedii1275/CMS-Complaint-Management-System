package com.dashenbank.cms.security;

import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileSecurityService {

    public static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    public static final String UPLOAD_DIR = "uploads";
    public static final Pattern TICKET_PATTERN = Pattern.compile("^[A-Za-z0-9/\\-_]{3,50}$");
    private static final long SIGNED_URL_TTL_SECONDS = 2 * 60 * 60;
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx", "txt", "csv");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a", "webm", "ogg");
    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "text/csv",
            "audio/mpeg",
            "audio/wav",
            "audio/x-wav",
            "audio/wave",
            "audio/mp4",
            "audio/webm",
            "audio/ogg",
            "application/octet-stream");

    private final Path uploadRoot;
    private final String signingSecret;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;

    public FileSecurityService(
            @Value("${file.upload-dir:" + UPLOAD_DIR + "}") String uploadDir,
            @Value("${app.jwtSecret}") String signingSecret,
            ComplaintSlaMetricsRepository slaMetricsRepository) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.signingSecret = signingSecret;
        this.slaMetricsRepository = slaMetricsRepository;
    }

    public Path uploadRoot() {
        return uploadRoot;
    }

    public Path resolveStoredFile(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()
                || storedFileName.contains("..")
                || storedFileName.contains("/")
                || storedFileName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        Path resolved = uploadRoot.resolve(storedFileName).normalize().toAbsolutePath();
        if (!resolved.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Path traversal rejected");
        }
        return resolved;
    }

    public void validateUpload(MultipartFile file, boolean audio) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds the 10MB limit");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank() || original.contains("..")
                || original.contains("/") || original.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid original file name");
        }
        String extension = extensionOf(original);
        Set<String> allowed = audio ? AUDIO_EXTENSIONS : union(DOCUMENT_EXTENSIONS, AUDIO_EXTENSIONS);
        if (!allowed.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type ." + extension + " is not allowed");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.isBlank() && !ALLOWED_MIME.contains(contentType)
                && !contentType.startsWith("audio/") && !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported MIME type");
        }
        if (contentType.contains("html") || contentType.contains("javascript") || contentType.contains("svg")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported MIME type");
        }
    }

    public String newStoredFileName(MultipartFile file) {
        return UUID.randomUUID() + "." + extensionOf(file.getOriginalFilename());
    }

    public String bindComplaintId(String complaintId, boolean authenticated) {
        if (complaintId != null && !complaintId.isBlank()) {
            if (!TICKET_PATTERN.matcher(complaintId.trim()).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid complaint ticket");
            }
            String ticket = complaintId.trim();
            if (authenticated || isKnownTicket(ticket) || ticket.toUpperCase(Locale.ROOT).startsWith("INTAKE")) {
                return ticket;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown complaint ticket");
        }
        return "INTAKE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public boolean isKnownTicket(String ticket) {
        return slaMetricsRepository.findByComplaintId(ticket).isPresent()
                || slaMetricsRepository.findByGeneralTicketId(ticket).isPresent();
    }

    public Map<String, String> signedDownload(String storedFileName, String originalName) {
        long exp = (System.currentTimeMillis() / 1000L) + SIGNED_URL_TTL_SECONDS;
        String sig = hmac(storedFileName + "|" + exp);
        String url = "/api/complaints/attachments/" + storedFileName + "?exp=" + exp + "&sig=" + sig;
        return Map.of("url", url, "fileName", originalName != null ? originalName : storedFileName);
    }

    public boolean hasValidDownloadGrant(String storedFileName, String exp, String sig) {
        if (storedFileName == null || exp == null || sig == null || exp.isBlank() || sig.isBlank()) {
            return false;
        }
        try {
            long expiry = Long.parseLong(exp);
            if (expiry < System.currentTimeMillis() / 1000L) {
                return false;
            }
            String expected = hmac(storedFileName + "|" + exp);
            String provided = sig.trim().toLowerCase(Locale.ROOT);
            byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
            byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
            return expectedBytes.length == providedBytes.length
                    && java.security.MessageDigest.isEqual(expectedBytes, providedBytes);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String safeContentDisposition(String originalName) {
        String cleaned = originalName == null ? "download" : originalName.replace("\"", "").replace("\r", "")
                .replace("\n", "").replace("..", "");
        if (cleaned.isBlank()) {
            cleaned = "download";
        }
        return "attachment; filename=\"" + cleaned + "\"";
    }

    public void ensureUploadDirectory() {
        try {
            Files.createDirectories(uploadRoot);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload directory unavailable");
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }

    private static String extensionOf(String original) {
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must have an extension");
        }
        return original.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static Set<String> union(Set<String> left, Set<String> right) {
        java.util.HashSet<String> merged = new java.util.HashSet<>(left);
        merged.addAll(right);
        return Set.copyOf(merged);
    }
}
