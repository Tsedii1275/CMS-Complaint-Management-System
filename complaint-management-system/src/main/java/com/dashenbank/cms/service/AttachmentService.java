package com.dashenbank.cms.service;

import com.dashenbank.cms.dto.AttachmentDTO;
import com.dashenbank.cms.model.Attachment;
import com.dashenbank.cms.repository.AttachmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx", "txt", "csv",
            "mp3", "wav", "m4a", "webm", "ogg");

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private com.dashenbank.cms.security.FileSecurityService fileSecurityService;

    public static final int MAX_ATTACHMENTS_PER_COMPLAINT = 5;

    public void checkAttachmentLimit(String complaintId) {
        if (complaintId != null && !complaintId.isBlank()) {
            List<Attachment> existing = attachmentRepository.findByComplaintIdOrderByUploadedAtDesc(complaintId);
            if (existing.size() >= MAX_ATTACHMENTS_PER_COMPLAINT) {
                log.warn(
                        "SECURITY REJECTION: Complaint ticket '{}' has reached maximum allowed attachments limit of {}",
                        complaintId, MAX_ATTACHMENTS_PER_COMPLAINT);
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Maximum allowed attachments limit (5) reached for this complaint.");
            }
        }
    }

    public AttachmentDTO saveAttachment(MultipartFile file, String complaintId, String uploadedBy) throws IOException {
        checkAttachmentLimit(complaintId);
        fileSecurityService.validateUpload(file, false, uploadedBy);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains("..")) {
            throw new IllegalArgumentException("Path traversal sequence detected in filename");
        }

        String storedFileName = storageService.storeFile(file);

        Attachment attachment = Attachment.builder()
                .complaintId(complaintId)
                .fileName(originalFilename != null ? originalFilename : storedFileName)
                .filePath(storedFileName)
                .fileType(file.getContentType())
                .uploadedBy(uploadedBy != null ? uploadedBy : "SYSTEM")
                .uploadedAt(LocalDateTime.now())
                .build();

        Attachment saved = attachmentRepository.save(attachment);

        // Audit Trail
        auditService.log(complaintId, "", "", "ATTACHMENT_UPLOADED", "ATTACHMENT",
                uploadedBy, "Uploaded attachment: " + saved.getFileName() + " [Size: " + file.getSize() + " bytes]", "",
                "", "");

        return mapToDTO(saved);
    }

    public Attachment saveAttachmentRecord(String complaintId, String fileName, String filePath, String fileType,
            String uploadedBy) {
        Attachment attachment = Attachment.builder()
                .complaintId(complaintId)
                .fileName(fileName)
                .filePath(filePath)
                .fileType(fileType)
                .uploadedBy(uploadedBy != null ? uploadedBy : "SYSTEM")
                .uploadedAt(LocalDateTime.now())
                .build();
        return attachmentRepository.save(attachment);
    }

    public List<AttachmentDTO> getAttachmentsByComplaintId(String complaintId) {
        if (complaintId == null) {
            return List.of();
        }
        return attachmentRepository.findByComplaintIdOrderByUploadedAtDesc(complaintId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Optional<Attachment> getAttachmentEntityById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return attachmentRepository.findById(id);
    }

    public Resource loadAttachmentResource(Attachment attachment) throws IOException {
        return storageService.loadFileAsResource(attachment.getFilePath());
    }

    public void deleteAttachment(Long id, String actor) {
        if (id == null) {
            return;
        }
        attachmentRepository.findById(id).ifPresent(att -> {
            storageService.deleteFile(att.getFilePath());
            attachmentRepository.deleteById(id);

            // Audit Trail
            auditService.log(att.getComplaintId(), "", "", "ATTACHMENT_DELETED", "ATTACHMENT",
                    actor, "Deleted attachment: " + att.getFileName(), "", "", "");
        });
    }

    public AttachmentDTO mapToDTO(Attachment att) {
        if (att == null)
            return null;
        return AttachmentDTO.builder()
                .id(att.getId())
                .complaintId(att.getComplaintId())
                .fileName(att.getFileName())
                .fileType(att.getFileType())
                .uploadedBy(att.getUploadedBy())
                .uploadedAt(att.getUploadedAt())
                .downloadUrl("/api/attachments/download/" + att.getId())
                .previewUrl("/api/attachments/preview/" + att.getId())
                .build();
    }
}
