package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.StageSlaEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StageSlaEventRepository extends JpaRepository<StageSlaEvent, Long> {

    Optional<StageSlaEvent> findByTaskId(String taskId);

    List<StageSlaEvent> findByProcessInstanceIdOrderByStartedAtAsc(String processInstanceId);

    List<StageSlaEvent> findByComplaintIdOrderByStartedAtAsc(String complaintId);

    List<StageSlaEvent> findByProcessInstanceIdIn(Collection<String> processInstanceIds);

    List<StageSlaEvent> findByStatusIn(Collection<String> statuses);

    long countByComplaintId(String complaintId);

    long countByComplaintIdAndStatus(String complaintId, String status);

    long countByProcessInstanceId(String processInstanceId);

    long countByProcessInstanceIdAndStatus(String processInstanceId, String status);
}
