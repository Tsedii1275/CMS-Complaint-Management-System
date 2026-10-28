package com.dashenbank.cms.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtherClassificationTest {

    @Test
    void missingOrUnknownTypeIsDisputeSoOldOtherItemsKeepTheirTab() {
        assertEquals(OtherClassification.DISPUTE, OtherClassification.normalize(null));
        assertEquals(OtherClassification.DISPUTE, OtherClassification.normalize(""));
        assertEquals(OtherClassification.DISPUTE, OtherClassification.normalize("whatever"));
        assertEquals(OtherClassification.DISPUTE, OtherClassification.typeOf(Map.of()));
        assertEquals(OtherClassification.DISPUTE, OtherClassification.typeOf(null));
        assertFalse(OtherClassification.isFeedback(null));
    }

    @Test
    void feedbackIsRecognisedCaseInsensitively() {
        assertEquals(OtherClassification.FEEDBACK, OtherClassification.normalize(" feedback "));
        assertTrue(OtherClassification.isFeedback("Feedback"));
        assertEquals(OtherClassification.FEEDBACK,
                OtherClassification.typeOf(Map.of(OtherClassification.KEY_OTHER_TYPE, "FEEDBACK")));
    }

    @Test
    void routesToTheRightTab() {
        assertEquals("Other", OtherClassification.targetTab(OtherClassification.DISPUTE));
        assertEquals("Feedback", OtherClassification.targetTab(OtherClassification.FEEDBACK));
        assertEquals("Ethics", OtherClassification.targetTab(OtherClassification.ETHICS));
        assertEquals("Dispute", OtherClassification.label(OtherClassification.DISPUTE));
        assertEquals("Feedback", OtherClassification.label(OtherClassification.FEEDBACK));
        assertEquals("Ethics and Conduct", OtherClassification.label(OtherClassification.ETHICS));
    }

    @Test
    void ethicsIsRecognisedAndIsNotADispute() {
        assertEquals(OtherClassification.ETHICS, OtherClassification.normalize(" ethics "));
        assertEquals(OtherClassification.ETHICS, OtherClassification.normalize("Ethics and Conduct"));
        assertTrue(OtherClassification.isEthics("ETHICS"));
        assertFalse(OtherClassification.isFeedback("ETHICS"));
        assertFalse(OtherClassification.isEthics("FEEDBACK"));
    }
}
