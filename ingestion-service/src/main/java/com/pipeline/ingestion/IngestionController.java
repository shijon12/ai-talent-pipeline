package com.pipeline.ingestion;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class IngestionController {

    private static final int MAX_EXTRACTED_CHARACTERS = 100_000;

    private final IngestionApplicationService applicationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplicationAcceptedResponse> submitApplication(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("targetJobId") String targetJobId
    ) {
        if (file.isEmpty()
                || isBlank(name)
                || isBlank(email)
                || isBlank(targetJobId)) {
            return ResponseEntity.badRequest().build();
        }

        String resumeText;
        try {
            Tika tika = new Tika();
            tika.setMaxStringLength(MAX_EXTRACTED_CHARACTERS);
            String detectedType = tika.detect(
                    file.getInputStream(),
                    file.getOriginalFilename()
            );
            if (!MediaType.APPLICATION_PDF_VALUE.equals(detectedType)) {
                return ResponseEntity.badRequest().build();
            }
            resumeText = tika.parseToString(file.getInputStream()).trim();
        }
        catch (IOException | TikaException exception) {
            log.warn(
                    "Unable to extract uploaded resume: {}",
                    exception.getClass().getSimpleName()
            );
            return ResponseEntity.badRequest().build();
        }

        if (resumeText.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.accepted().body(applicationService.accept(
                name,
                email,
                resumeText,
                targetJobId
        ));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
