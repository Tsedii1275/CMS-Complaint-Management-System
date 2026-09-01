package com.dashenbank.cms.controller;

import com.dashenbank.cms.dto.AttachmentDTO;
import com.dashenbank.cms.model.Attachment;
import com.dashenbank.cms.service.AttachmentService;
import com.dashenbank.cms.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attachments")
@Slf4j
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private AuditService auditService;

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("complaintId") String complaintId,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {
        try {
            String uploader = (uploadedBy != null && !uploadedBy.isBlank()) ? uploadedBy : getCurrentUsername();
            AttachmentDTO dto = attachmentService.saveAttachment(file, complaintId, uploader);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Attachment upload validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to upload attachment: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/complaint/{complaintId}")
    public ResponseEntity<List<AttachmentDTO>> getAttachmentsByComplaintId(@PathVariable String complaintId) {
        List<AttachmentDTO> attachments = attachmentService.getAttachmentsByComplaintId(complaintId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadAttachmentById(@PathVariable Long id) {
        var opt = attachmentService.getAttachmentEntityById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Attachment att = opt.get();
        try {
            Resource resource = attachmentService.loadAttachmentResource(att);
            String contentType = att.getFileType() != null ? att.getFileType() : "application/octet-stream";

            // Audit Trail
            auditService.log(att.getComplaintId(), "", "", "ATTACHMENT_DOWNLOADED", "ATTACHMENT",
                    getCurrentUsername(), "Downloaded attachment: " + att.getFileName(), "", "", "");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + att.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error downloading file ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewAttachmentById(@PathVariable Long id) {
        var opt = attachmentService.getAttachmentEntityById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Attachment att = opt.get();
        try {
            Resource resource = attachmentService.loadAttachmentResource(att);
            String contentType = att.getFileType() != null ? att.getFileType() : "application/octet-stream";

            // Audit Trail
            auditService.log(att.getComplaintId(), "", "", "ATTACHMENT_PREVIEWED", "ATTACHMENT",
                    getCurrentUsername(), "Previewed attachment: " + att.getFileName(), "", "", "");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + att.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error previewing file ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAttachment(@PathVariable Long id) {
        String actor = getCurrentUsername();
        attachmentService.deleteAttachment(id, actor);
        return ResponseEntity.ok(Map.of("message", "Attachment deleted successfully", "id", id));
    }
}
