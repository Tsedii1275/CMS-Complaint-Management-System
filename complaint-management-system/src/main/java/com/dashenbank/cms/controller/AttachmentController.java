package com.dashenbank.cms.controller;

import com.dashenbank.cms.dto.AttachmentDTO;
import com.dashenbank.cms.model.Attachment;
import com.dashenbank.cms.security.FileSecurityService;
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

    @Autowired
    private FileSecurityService fileSecurityService;

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("complaintId") String complaintId) {
        try {
            fileSecurityService.validateUpload(file, false);
            String ticket = fileSecurityService.bindComplaintId(complaintId, true);
            AttachmentDTO dto = attachmentService.saveAttachment(file, ticket, getCurrentUsername());
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Attachment upload validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to upload attachment: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed"));
        }
    }

    @GetMapping("/complaint/{complaintId}")
    public ResponseEntity<List<AttachmentDTO>> getAttachmentsByComplaintId(@PathVariable String complaintId) {
        List<AttachmentDTO> attachments = attachmentService.getAttachmentsByComplaintId(complaintId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadAttachmentById(@PathVariable Long id) {
        return serveAttachment(id, "ATTACHMENT_DOWNLOADED", "Downloaded attachment: ");
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewAttachmentById(@PathVariable Long id) {
        return serveAttachment(id, "ATTACHMENT_PREVIEWED", "Previewed attachment: ");
    }

    private ResponseEntity<Resource> serveAttachment(Long id, String action, String auditPrefix) {
        var opt = attachmentService.getAttachmentEntityById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Attachment att = opt.get();
        try {
            Resource resource = attachmentService.loadAttachmentResource(att);
            String contentType = att.getFileType() != null ? att.getFileType() : "application/octet-stream";
            auditService.log(att.getComplaintId(), "", "", action, "ATTACHMENT",
                    getCurrentUsername(), auditPrefix + att.getFileName(), "", "", "");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, fileSecurityService.safeContentDisposition(att.getFileName()))
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving file ID {}: {}", id, e.getMessage());
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
