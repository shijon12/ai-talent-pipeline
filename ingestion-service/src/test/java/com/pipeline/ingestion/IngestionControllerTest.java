package com.pipeline.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class IngestionControllerTest {

    private IngestionApplicationService applicationService;
    private IngestionController controller;

    @BeforeEach
    void setUp() {
        applicationService = org.mockito.Mockito.mock(
                IngestionApplicationService.class
        );
        controller = new IngestionController(applicationService);
    }

    @Test
    void extractsPdfAndHandsApplicationToAtomicService() throws IOException {
        MockMultipartFile resume = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                createPdf("Java 17, Spring Boot, Kafka, and PostgreSQL.")
        );
        ApplicationAcceptedResponse accepted = new ApplicationAcceptedResponse(
                "candidate-123",
                CandidateLifecycleStatus.INGESTED
        );
        when(applicationService.accept(
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(accepted);

        var response = controller.submitApplication(
                resume,
                "Test Candidate",
                "candidate@example.com",
                "job-101"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(accepted);
        ArgumentCaptor<String> resumeText = ArgumentCaptor.forClass(String.class);
        verify(applicationService).accept(
                org.mockito.ArgumentMatchers.eq("Test Candidate"),
                org.mockito.ArgumentMatchers.eq("candidate@example.com"),
                resumeText.capture(),
                org.mockito.ArgumentMatchers.eq("job-101")
        );
        assertThat(resumeText.getValue()).contains("Java 17", "Kafka");
    }

    @Test
    void rejectsEmptyUploadWithoutPersisting() {
        MockMultipartFile empty = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                new byte[0]
        );

        var response = controller.submitApplication(
                empty,
                "Test Candidate",
                "candidate@example.com",
                "job-101"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(applicationService, never()).accept(
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void rejectsCorruptPdfWithoutPersisting() {
        MockMultipartFile corrupt = new MockMultipartFile(
                "file",
                "corrupt.pdf",
                "application/pdf",
                "%PDF-1.4\nthis is not a valid PDF"
                        .getBytes(StandardCharsets.UTF_8)
        );

        var response = controller.submitApplication(
                corrupt,
                "Test Candidate",
                "candidate@example.com",
                "job-101"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(applicationService, never()).accept(
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void rejectsNonPdfUploadWithoutPersisting() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "Java and Spring Boot".getBytes(StandardCharsets.UTF_8)
        );

        var response = controller.submitApplication(
                textFile,
                "Test Candidate",
                "candidate@example.com",
                "job-101"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(applicationService, never()).accept(
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    private byte[] createPdf(String text) throws IOException {
        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(
                    document,
                    page
            )) {
                content.beginText();
                content.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                        12
                );
                content.newLineAtOffset(72, 720);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
