package com.dashenbank.cms.security;

import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileSecurityServiceTest {

    private static final String SECRET = "unit-test-jwt-secret-key-32bytes-min";

    @TempDir
    Path uploadDir;

    private FileSecurityService service;
    private ComplaintSlaMetricsRepository slaMetricsRepository;

    @BeforeEach
    void setUp() {
        slaMetricsRepository = mock(ComplaintSlaMetricsRepository.class);
        when(slaMetricsRepository.findByComplaintId(anyString())).thenReturn(Optional.empty());
        when(slaMetricsRepository.findByGeneralTicketId(anyString())).thenReturn(Optional.empty());
        service = new FileSecurityService(uploadDir.toString(), SECRET, slaMetricsRepository);
    }

    @Test
    void resolveStoredFileRejectsPathTraversal() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resolveStoredFile("../secret.txt"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void resolveStoredFileRejectsDirectorySeparators() {
        assertThrows(ResponseStatusException.class, () -> service.resolveStoredFile("a/b.txt"));
        assertThrows(ResponseStatusException.class, () -> service.resolveStoredFile("a\\b.txt"));
    }

    @Test
    void resolveStoredFileStaysInsideUploadRoot() {
        Path resolved = service.resolveStoredFile("safe.pdf");
        assertTrue(resolved.startsWith(uploadDir.toAbsolutePath().normalize()));
    }

    @Test
    void publicUploadRejectsUnknownTicket() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.bindComplaintId("HACK-99", false));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void missingTicketBindsToIntakePrefix() {
        String ticket = service.bindComplaintId(null, false);
        assertTrue(ticket.startsWith("INTAKE-"));
    }

    @Test
    void validateUploadRejectsHtmlAndOversizedTypes() {
        MockMultipartFile html = new MockMultipartFile("file", "page.html", "text/html", "<script>".getBytes());
        assertThrows(ResponseStatusException.class, () -> service.validateUpload(html, false));

        MockMultipartFile svg = new MockMultipartFile("file", "icon.svg", "image/svg+xml", "<svg/>".getBytes());
        assertThrows(ResponseStatusException.class, () -> service.validateUpload(svg, false));
    }

    @Test
    void signedDownloadGrantRoundTrip() {
        Map<String, String> signed = service.signedDownload("stored.pdf", "evidence.pdf");
        String url = signed.get("url");
        String exp = url.substring(url.indexOf("exp=") + 4, url.indexOf("&sig="));
        String sig = url.substring(url.indexOf("sig=") + 4);
        assertTrue(service.hasValidDownloadGrant("stored.pdf", exp, sig));
        assertFalse(service.hasValidDownloadGrant("stored.pdf", "1", sig));
        assertFalse(service.hasValidDownloadGrant("other.pdf", exp, sig));
    }
}
