package com.dashenbank.cms.service;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.flowable.validation.ProcessValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the deployed process contains the Senior Manager / Service Quality / CXO review loop. */
class InvestigationLoopProcessTest {

    private static BpmnModel model;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = new ClassPathResource("processes/cMS (2).bpmn20.xml").getInputStream()) {
            model = new BpmnXMLConverter().convertToBpmnModel(new InputStreamSource(in), false, false);
        }
    }

    private static SequenceFlow flow(String id) {
        FlowElement element = model.getFlowElement(id);
        assertNotNull(element, id);
        return (SequenceFlow) element;
    }

    @Test
    void processIsValid() {
        var errors = new ProcessValidatorFactory().createDefaultProcessValidator().validate(model);
        assertTrue(errors.stream().noneMatch(e -> !e.isWarning()), errors.toString());
    }

    @Test
    void serviceQualityEitherSendsToCxoOrReturnsToSeniorManager() {
        assertEquals("Gateway_SqDecision", flow("SequenceFlow_SQ_To_Decision").getTargetRef());
        assertEquals("FormTask_CEX", flow("SequenceFlow_SQ_To_CEX").getTargetRef());
        assertEquals("FormTask_SM_Closure", flow("SequenceFlow_SQ_ReturnToSM").getTargetRef());
        assertEquals("SequenceFlow_SQ_To_CEX", ((ExclusiveGateway) model.getFlowElement("Gateway_SqDecision")).getDefaultFlow());
    }

    @Test
    void cxoEitherSendsToAuditOrReturnsToServiceQuality() {
        assertEquals("FormTask_48", flow("SequenceFlow_CXO_SendToAudit").getTargetRef());
        assertEquals("FormTask_SQ_Return", flow("SequenceFlow_CXO_ReturnToSQ").getTargetRef());
        assertEquals("FormTask_SM_Closure", flow("SequenceFlow_SQ_Return_To_SM").getTargetRef());
    }

    @Test
    void seniorManagerEitherClosesOrSendsBackToServiceQualityAndTheLoopRepeats() {
        assertEquals("Gateway_SmDecision", flow("SequenceFlow_SM_Closure_To_Decision").getTargetRef());
        assertEquals("EndNoneEvent_93", flow("SequenceFlow_SM_Closure_End").getTargetRef());
        assertEquals("FormTask_SQ_Review", flow("SequenceFlow_SM_ResendToSQ").getTargetRef());
    }
}
