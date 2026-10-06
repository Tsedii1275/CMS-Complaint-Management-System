package com.dashenbank.cms.service;

import com.dashenbank.cms.model.StageSlaEvent;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.repository.SlaBreachRecordRepository;
import com.dashenbank.cms.repository.StageSlaEventRepository;
import org.flowable.engine.HistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StageSlaLedgerServiceTest {

    private StageSlaEventRepository eventRepository;
    private SlaTrackingService slaTrackingService;
    private BusinessHoursService businessHoursService;
    private StageSlaLedgerService ledger;

    @BeforeEach
    void setUp() {
        eventRepository = mock(StageSlaEventRepository.class);
        slaTrackingService = mock(SlaTrackingService.class);
        businessHoursService = mock(BusinessHoursService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<HistoryService> history = mock(ObjectProvider.class);
        when(history.getIfAvailable()).thenReturn(null);
        when(eventRepository.save(any(StageSlaEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        SlaBreachRecordRepository breachRepo = mock(SlaBreachRecordRepository.class);
        when(breachRepo.existsByTaskId(anyString())).thenReturn(false);
        when(breachRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ledger = new StageSlaLedgerService(eventRepository, mock(ComplaintSlaMetricsRepository.class),
                breachRepo, slaTrackingService, businessHoursService, history);
    }

    @Test
    void cxoTaskUsesPolicyMatrixMinutes() {
        when(eventRepository.findByTaskId("t-cex")).thenReturn(Optional.empty());
        when(slaTrackingService.calculateStageSlaMinutes("FormTask_CEX", "GENERAL", null)).thenReturn(180);
        StageSlaEvent event = ledger.onTaskCreated("pi-1", "DBC-1", "t-cex", "FormTask_CEX", "cxo.user");
        assertEquals("CHIEF_EXPERIENCE_REVIEW", event.getCanonicalStage());
        assertEquals(180, event.getAllowedMinutes());
        assertEquals(StageSlaEvent.STATUS_IN_PROGRESS, event.getStatus());
    }

    @Test
    void openEventRereadsPolicyMinutesOnEvaluate() {
        StageSlaEvent open = StageSlaEvent.builder()
                .taskId("t-wu")
                .processInstanceId("pi-2")
                .complaintId("DBC-2")
                .taskDefinitionKey("FormTask_57")
                .canonicalStage("WORK_UNIT_RESOLUTION")
                .startedAt(LocalDateTime.now().minusHours(5))
                .allowedMinutes(240)
                .status(StageSlaEvent.STATUS_IN_PROGRESS)
                .breachRecorded(false)
                .build();
        when(eventRepository.findByStatusIn(any())).thenReturn(List.of(open));
        when(slaTrackingService.calculateStageSlaMinutes("FormTask_57", "GENERAL", null)).thenReturn(60);
        when(businessHoursService.calculateElapsedBusinessMinutes(any(), any())).thenReturn(90L);

        AtomicReference<StageSlaEvent> saved = new AtomicReference<>();
        when(eventRepository.save(any(StageSlaEvent.class))).thenAnswer(inv -> {
            saved.set(inv.getArgument(0));
            return inv.getArgument(0);
        });

        ledger.evaluateOpenEvents();
        assertEquals(60, saved.get().getAllowedMinutes());
        assertEquals(StageSlaEvent.STATUS_BREACHED, saved.get().getStatus());
        assertTrue(Boolean.TRUE.equals(saved.get().getBreachRecorded())
                || StageSlaEvent.STATUS_BREACHED.equals(saved.get().getStatus()));
    }
}
