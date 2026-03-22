package com.accounting.ocr.dto;

import com.accounting.ocr.model.DocumentType;
import com.accounting.ocr.model.ProcessingStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private String fileId;
    private String originalFileName;
    private DocumentType documentType;
    private ProcessingStatus status;
    private String documentNumber;
    private LocalDate documentDate;
    private BigDecimal totalAmount;
    private String counterpartyName;
    private String counterpartyInn;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String errorMessage;
}