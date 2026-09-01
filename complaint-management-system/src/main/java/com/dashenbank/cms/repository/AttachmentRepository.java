package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByComplaintId(String complaintId);

    List<Attachment> findByComplaintIdOrderByUploadedAtDesc(String complaintId);

    void deleteByComplaintId(String complaintId);
}
