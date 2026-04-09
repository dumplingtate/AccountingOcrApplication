package com.accounting.ocr.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false, unique = true)
    private String fileId;  // UUID

    @Column(nullable = false)
    private String storagePath;

    private Long fileSize;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private String documentNumber;

    private LocalDate documentDate;

    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    private String extractedText;  // Raw OCR text

    @Column(columnDefinition = "TEXT")
    private String rawMlResponse;  // Raw response from ML service

    @ManyToOne
    @JoinColumn(name = "counterparty_id")
    private Counterparty counterparty;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ProcessingStatus.UPLOADED;
        }
    }

    public void markProcessed() {
        this.status = ProcessingStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markError(String error) {
        this.status = ProcessingStatus.ERROR;
        this.errorMessage = error;
        this.processedAt = LocalDateTime.now();
    }
}