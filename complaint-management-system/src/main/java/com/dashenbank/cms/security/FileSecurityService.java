package com.dashenbank.cms.security;

import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class FileSecurityService {

    public static final long MAX_DOCUMENT_UPLOAD_BYTES = 10L * 1024 * 1024; // 10 MB for documents/images
    public static final long MAX_AUDIO_UPLOAD_BYTES = 25L * 1024 * 1024; // 25 MB for voice evidence
    public static final long MAX_UPLOAD_BYTES = MAX_AUDIO_UPLOAD_BYTES;

    public static final String UPLOAD_DIR = "/opt/cms/uploads";
    public static final Pattern TICKET_PATTERN = Pattern.compile("^[A-Za-z0-9/\\-_]{3,50}$");
    private static final long SIGNED_URL_TTL_SECONDS = 2 * 60 * 60;

    // 1. Strict Whitelist of Approved File Extensions (Documents + Audio Evidence)
    public static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "docx", "xlsx");
    public static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a");
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "docx", "xlsx", "mp3", "wav", "m4a");

    // Explicitly Blacklisted dangerous extensions for audit logging
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "ps1", "sh", "jar", "war", "jsp", "php", "asp",
            "aspx", "html", "htm", "js", "svg", "zip", "rar", "7z", "vbs", "py", "pl", "cgi");

    // 2. Strict Whitelist of Approved Content-Types (MIME)
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/x-wav",
            "audio/wave",
            "audio/mp4",
            "audio/x-m4a",
            "audio/m4a");

    private final Path uploadRoot;
    private final String signingSecret;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;

    @Autowired(required = false)
    private MalwareScannerService malwareScannerService;

    public FileSecurityService(
            @Value("${file.upload-dir:" + UPLOAD_DIR + "}") String uploadDir,
            @Value("${app.jwtSecret:DashenCMSSecretKeyForHMACSigningTokenGuard12345}") String signingSecret,
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
                || storedFileName.contains("\\")
                || storedFileName.contains("\0")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        Path resolved = uploadRoot.resolve(storedFileName).normalize().toAbsolutePath();
        if (!resolved.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return resolved;
    }

    /**
     * Primary backend validation for all file uploads.
     */
    public void validateUpload(MultipartFile file, boolean audioAllowed, String uploader, String clientIp,
            String ticketId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file cannot be empty");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank() || original.contains("..")
                || original.contains("/") || original.contains("\\") || original.contains("\0")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name structure");
        }

        String extension = extensionOf(original);
        boolean isAudio = AUDIO_EXTENSIONS.contains(extension);

        // File Size Restrictions (10MB for documents/images, 25MB for audio evidence)
        if (isAudio) {
            if (file.getSize() > MAX_AUDIO_UPLOAD_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Uploaded audio file exceeds the maximum allowed size of 25 MB.");
            }
        } else {
            if (file.getSize() > MAX_DOCUMENT_UPLOAD_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Uploaded file exceeds the maximum allowed size of 10 MB.");
            }
        }

        // Extension Rules & Double Extension Check
        validateExtensionRules(original, extension);

        // MIME Type Rules & Extension Cross-Check
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT).trim();
        validateMimeTypeRules(extension, contentType);

        // File Content Magic Byte Signature Verification
        validateMagicBytes(file, extension);

        // Deep Office OpenXML Structure Check for DOCX and XLSX
        if (extension.equals("docx") || extension.equals("xlsx")) {
            validateOfficeOpenXmlStructure(file, extension);
        }

        // Malware & Virus Scanning (ClamAV + Heuristics)
        if (malwareScannerService != null) {
            malwareScannerService.scanFile(file, original, uploader, clientIp, ticketId);
        }
    }

    public void validateUpload(MultipartFile file, boolean audioAllowed, String uploader) {
        validateUpload(file, audioAllowed, uploader, "UNKNOWN", null);
    }

    public void validateUpload(MultipartFile file, boolean audioAllowed) {
        validateUpload(file, audioAllowed, "SYSTEM", "UNKNOWN", null);
    }

    private void validateExtensionRules(String originalName, String extension) {
        String[] parts = originalName.toLowerCase(Locale.ROOT).split("\\.");
        if (parts.length > 2) {
            for (int i = 1; i < parts.length - 1; i++) {
                if (DANGEROUS_EXTENSIONS.contains(parts[i]) || ALLOWED_EXTENSIONS.contains(parts[i])) {
                    log.warn("SECURITY ALERT: Double extension detected in file '{}'", originalName);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "File upload rejected: Suspicious double extension format");
                }
            }
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("SECURITY REJECTION: Extension '.{}' not allowed for file '{}'", extension, originalName);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file type is not permitted.");
        }
    }

    private void validateMimeTypeRules(String extension, String contentType) {
        if (contentType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing file Content-Type header");
        }

        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            log.warn("SECURITY REJECTION: Non-whitelisted MIME type '{}' for extension '.{}'", contentType, extension);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file type is not permitted.");
        }

        boolean matched = switch (extension) {
            case "pdf" -> contentType.equals("application/pdf");
            case "jpg", "jpeg" -> contentType.equals("image/jpeg") || contentType.equals("image/jpg");
            case "png" -> contentType.equals("image/png");
            case "docx" ->
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "xlsx" -> contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "mp3" -> contentType.equals("audio/mpeg") || contentType.equals("audio/mp3");
            case "wav" -> contentType.equals("audio/wav") || contentType.equals("audio/x-wav")
                    || contentType.equals("audio/wave");
            case "m4a" ->
                contentType.equals("audio/mp4") || contentType.equals("audio/x-m4a") || contentType.equals("audio/m4a");
            default -> false;
        };

        if (!matched) {
            log.warn("SECURITY ALERT: Extension/MIME spoofing detected! Extension='.{}', Content-Type='{}'", extension,
                    contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file type is not permitted.");
        }
    }

    /**
     * Magic Number Validation for Documents, Images, and Audio Evidence
     */
    private void validateMagicBytes(MultipartFile file, String extension) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(512);
            if (header.length < 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content is corrupted or unreadable");
            }

            boolean validSignature = switch (extension) {
                case "pdf" -> // %PDF
                    header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46;
                case "png" -> // 89 PNG
                    header.length >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50
                            && header[2] == 0x4E && header[3] == 0x47;
                case "jpg", "jpeg" -> // FF D8 FF
                    (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
                case "docx", "xlsx" -> // PK\x03\x04
                    header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04;
                case "mp3" -> // ID3 tag OR MPEG frame sync header (0xFFE0+)
                    (header[0] == 0x49 && header[1] == 0x44 && header[2] == 0x33) ||
                            ((header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0);
                case "wav" -> // RIFF ... WAVE
                    header.length >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46
                            && header[3] == 0x46
                            && header[8] == 0x57 && header[9] == 0x41 && header[10] == 0x56 && header[11] == 0x45;
                case "m4a" -> // ISO ftyp M4A box
                    isM4aMagicHeader(header);
                default -> false;
            };

            if (!validSignature) {
                log.warn("SECURITY ALERT: Magic byte validation failed for extension '.{}'", extension);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file type is not permitted.");
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inspecting magic bytes: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to verify file signature");
        }
    }

    private boolean isM4aMagicHeader(byte[] header) {
        if (header.length < 12)
            return false;
        for (int i = 0; i <= header.length - 8; i++) {
            if (header[i] == 0x66 && header[i + 1] == 0x74 && header[i + 2] == 0x79 && header[i + 3] == 0x70) { // "ftyp"
                String brand = new String(header, i + 4, Math.min(16, header.length - (i + 4)),
                        StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
                if (brand.contains("m4a") || brand.contains("mp4") || brand.contains("isom") || brand.contains("dash")
                        || brand.contains("audio")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateOfficeOpenXmlStructure(MultipartFile file, String extension) {
        long totalUncompressedBytes = 0;
        int totalEntries = 0;
        boolean hasContentTypes = false;
        boolean hasOfficeRoot = false;

        try (InputStream is = file.getInputStream();
                ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                totalEntries++;
                if (totalEntries > 500) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "File upload rejected: Archive exceeds max entry count");
                }

                String entryName = entry.getName().toLowerCase(Locale.ROOT);
                if (entryName.equals("[content_types].xml")) {
                    hasContentTypes = true;
                }
                if (extension.equals("docx") && entryName.equals("word/document.xml")) {
                    hasOfficeRoot = true;
                }
                if (extension.equals("xlsx") && entryName.equals("xl/workbook.xml")) {
                    hasOfficeRoot = true;
                }

                byte[] buf = new byte[4096];
                long entryUncompressedBytes = 0;
                int read;
                while ((read = zis.read(buf)) != -1) {
                    entryUncompressedBytes += read;
                    totalUncompressedBytes += read;
                    if (totalUncompressedBytes > 30L * 1024 * 1024) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "File upload rejected: Decompressed archive size exceeds limit");
                    }
                }

                if (entry.getCompressedSize() > 0) {
                    long ratio = entryUncompressedBytes / entry.getCompressedSize();
                    if (ratio > 100) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "File upload rejected: Abnormal compression ratio detected");
                    }
                }

                zis.closeEntry();
            }

            if (!hasContentTypes || !hasOfficeRoot) {
                log.warn("SECURITY REJECTION: File with extension .{} lacks mandatory Office OpenXML structure",
                        extension);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "File upload rejected: Generic ZIP archive or invalid Office structure");
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("SECURITY REJECTION: Failed to parse Office OpenXML archive for extension .{} - {}", extension,
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File upload rejected: Invalid or unreadable document archive");
        }
    }

    public String newStoredFileName(MultipartFile file) {
        String ext = extensionOf(file.getOriginalFilename());
        String randomHash = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "attachment_" + randomHash + "." + ext;
    }

    public String bindComplaintId(String complaintId, boolean authenticated) {
        if (complaintId != null && !complaintId.isBlank()) {
            if (!TICKET_PATTERN.matcher(complaintId.trim()).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid complaint ticket format");
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
        if (originalName == null || originalName.isBlank()) {
            return "attachment; filename=\"download\"";
        }
        String sanitized = originalName
                .replaceAll("[\\r\\n\"';\\\\/\0]", "")
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (sanitized.isBlank()) {
            sanitized = "download";
        }
        return "attachment; filename=\"" + sanitized + "\"";
    }

    public void ensureUploadDirectory() {
        try {
            Files.createDirectories(uploadRoot);
            try {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");
                Files.setPosixFilePermissions(uploadRoot, perms);
            } catch (Exception ignored) {
                // Windows OS
            }
        } catch (Exception e) {
            log.error("Failed to create secure upload directory at {}: {}", uploadRoot, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Secure upload directory unavailable");
        }
    }

    public void setNonExecutablePermissions(Path targetPath) {
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r-----");
            Files.setPosixFilePermissions(targetPath, perms);
        } catch (Exception ignored) {
            // Windows OS
        }
    }

    public String extractClientIp(HttpServletRequest request) {
        if (request == null)
            return "UNKNOWN";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "UNKNOWN";
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC signing service unavailable", e);
        }
    }

    private static String extensionOf(String original) {
        if (original == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename cannot be null");
        }
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must have a valid extension");
        }
        return original.substring(dot + 1).toLowerCase(Locale.ROOT).trim();
    }
}
