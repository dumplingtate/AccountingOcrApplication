package com.accounting.ocr.service.ml;

import com.accounting.ocr.exception.MLServiceUnavailableException;
import com.accounting.ocr.service.FileStorageService;
import com.accounting.ocr.service.ml.dto.MLRequest;
import com.accounting.ocr.service.ml.dto.MLResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class MLServiceClient {

    private final RestTemplate restTemplate;
    private final FileStorageService fileStorageService;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    public MLResponse classifyDocument(String fileId) {
        log.info("Calling ML service for document: {}", fileId);

        try {
            // Prepare file for sending
            Path filePath = fileStorageService.getFilePath(fileId);
            FileSystemResource fileResource = new FileSystemResource(filePath.toFile());

            // Build multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            body.add("file_id", fileId);
            body.add("return_full_text", "true");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Call ML service
            String url = mlServiceUrl + "/classify";
            ResponseEntity<MLResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    MLResponse.class
            );

            if (response.getBody() == null) {
                throw new MLServiceUnavailableException("ML service returned empty response");
            }

            log.info("ML service response received: type={}, confidence={}",
                    response.getBody().getDocumentType(),
                    response.getBody().getConfidence());

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to call ML service", e);
            throw new MLServiceUnavailableException(
                    "ML service is unavailable: " + e.getMessage(), e
            );
        }
    }

    public boolean isMlServiceHealthy() {
        try {
            String url = mlServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("ML service health check failed", e);
            return false;
        }
    }
}