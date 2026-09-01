package com.dashenbank.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDTO {
    private Long id;
    private String complaintId;
    private String fileName;
    private String fileType;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    private String previewUrl;
}
