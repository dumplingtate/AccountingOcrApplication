package com.accounting.ocr.controller;

import com.accounting.ocr.dto.DocumentResponse;
import com.accounting.ocr.dto.UploadDocumentRequest;
import com.accounting.ocr.service.DocumentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "Endpoints for document upload and processing")
public class DocumentController {

    private final DocumentProcessingService processingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and process a document", description = "Uploads a document (PDF or image) and processes it with ML service")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or validation failed"),
            @ApiResponse(responseCode = "413", description = "File too large"),
            @ApiResponse(responseCode = "503", description = "ML service unavailable")
    })
    public ResponseEntity<DocumentResponse> uploadDocument(
            @Parameter(description = "Document file (PDF or image)", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Optional description")
            @RequestParam(value = "description", required = false) String description) {

        log.info("Received upload request: file={}, size={}",
                file.getOriginalFilename(), file.getSize());

        DocumentResponse response = processingService.processDocument(file, description);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID", description = "Retrieves document details and processing status")
    public ResponseEntity<DocumentResponse> getDocument(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long id) {

        DocumentResponse response = processingService.getDocumentStatus(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all documents", description = "Retrieves list of all processed documents")
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        List<DocumentResponse> documents = processingService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is running")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Document OCR Service is running");
    }
}