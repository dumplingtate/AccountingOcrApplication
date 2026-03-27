package com.accounting.ocr.service;

import com.accounting.ocr.dto.DocumentResponse;
import com.accounting.ocr.dto.ValidationError;
import com.accounting.ocr.exception.DocumentProcessingException;
import com.accounting.ocr.model.*;
import com.accounting.ocr.repository.CounterpartyRepository;
import com.accounting.ocr.repository.DocumentRepository;
import com.accounting.ocr.service.ml.MLServiceClient;
import com.accounting.ocr.service.ml.dto.MLResponse;
import com.accounting.ocr.service.validation.ExtractedFieldsValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentProcessingService {

    private final FileStorageService fileStorageService;
    private final MLServiceClient mlServiceClient;
    private final DocumentRepository documentRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final ExtractedFieldsValidator fieldsValidator;
    private final ObjectMapper objectMapper;

    public DocumentResponse processDocument(MultipartFile file, String description) {
        log.info("Starting document processing for file: {}", file.getOriginalFilename());

        // Step 1: Store file
        String fileId = fileStorageService.storeFile(file);

        // Step 2: Create document record
        Document document = createDocumentRecord(file, fileId, description);
        document = documentRepository.save(document);
        log.info("Document record created with ID: {}", document.getId());

        // Step 3: Call ML service for classification and extraction
        MLResponse mlResponse;
        try {
            document.setStatus(ProcessingStatus.ML_PROCESSING);
            documentRepository.save(document);

            mlResponse = mlServiceClient.classifyDocument(fileId);

            // Store raw ML response
            document.setRawMlResponse(objectMapper.writeValueAsString(mlResponse));
            document.setExtractedText(mlResponse.getRawText());

        } catch (Exception e) {
            log.error("ML processing failed for document: {}", fileId, e);
            document.markError("ML processing failed: " + e.getMessage());
            documentRepository.save(document);
            throw new DocumentProcessingException("ML processing failed", e);
        }

        // Step 4: Validate extracted data
        List<ValidationError> validationErrors = fieldsValidator.validate(mlResponse);

        if (!validationErrors.isEmpty()) {
            log.warn("Validation failed for document {}. Errors: {}", fileId, validationErrors);
            document.setStatus(ProcessingStatus.VALIDATION_FAILED);
            document.setErrorMessage("Validation failed: " + validationErrors.toString());
            documentRepository.save(document);
            throw new DocumentProcessingException("Validation failed: " + validationErrors);
        }

        // Step 5: Find or create counterparty
        Counterparty counterparty = findOrCreateCounterparty(mlResponse);

        // Step 6: Update document with extracted data
        updateDocumentWithMlData(document, mlResponse, counterparty);
        document.markProcessed();
        document = documentRepository.save(document);

        log.info("Document processing completed successfully. ID: {}", document.getId());

        // Step 7: Build and return response
        return buildDocumentResponse(document);
    }

    private Document createDocumentRecord(MultipartFile file, String fileId, String description) {
        Document document = new Document();
        document.setOriginalFileName(file.getOriginalFilename());
        document.setFileId(fileId);
        document.setStoragePath(fileStorageService.getFilePath(fileId).toString());
        document.setFileSize(file.getSize());
        document.setStatus(ProcessingStatus.UPLOADED);
        document.setDocumentType(DocumentType.UNKNOWN);
        return document;
    }

    private Counterparty findOrCreateCounterparty(MLResponse mlResponse) {
        if (mlResponse.getCounterpartyInn() == null || mlResponse.getCounterpartyInn().isEmpty()) {
            return null;
        }

        return counterpartyRepository.findByInn(mlResponse.getCounterpartyInn())
                .orElseGet(() -> {
                    Counterparty newCounterparty = new Counterparty();
                    newCounterparty.setInn(mlResponse.getCounterpartyInn());
                    newCounterparty.setKpp(mlResponse.getCounterpartyKpp());
                    newCounterparty.setName(mlResponse.getCounterpartyName());
                    return counterpartyRepository.save(newCounterparty);
                });
    }

    private void updateDocumentWithMlData(Document document, MLResponse mlResponse, Counterparty counterparty) {
        document.setDocumentType(mlResponse.getDocumentType());
        document.setDocumentNumber(mlResponse.getDocumentNumber());
        document.setDocumentDate(mlResponse.getDocumentDate());
        document.setTotalAmount(mlResponse.getTotalAmount());
        document.setCounterparty(counterparty);
    }

    private DocumentResponse buildDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileId(document.getFileId())
                .originalFileName(document.getOriginalFileName())
                .documentType(document.getDocumentType())
                .status(document.getStatus())
                .documentNumber(document.getDocumentNumber())
                .documentDate(document.getDocumentDate())
                .totalAmount(document.getTotalAmount())
                .counterpartyName(document.getCounterparty() != null ? document.getCounterparty().getName() : null)
                .counterpartyInn(document.getCounterparty() != null ? document.getCounterparty().getInn() : null)
                .createdAt(document.getCreatedAt())
                .processedAt(document.getProcessedAt())
                .errorMessage(document.getErrorMessage())
                .build();
    }

    public DocumentResponse getDocumentStatus(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));
        return buildDocumentResponse(document);
    }

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::buildDocumentResponse)
                .toList();
    }
}