package com.accounting.ocr.service.ml.dto;

import com.accounting.ocr.model.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLResponse {

    private DocumentType documentType;
    private Double confidence;

    private String documentNumber;
    private LocalDate documentDate;
    private BigDecimal totalAmount;
    private BigDecimal vatAmount;

    private String counterpartyName;
    private String counterpartyInn;
    private String counterpartyKpp;

    private Map<String, String> additionalFields;

    private String rawText;
    private String processingTime;

    public boolean isValid() {
        return confidence != null && confidence > 0.5;
    }
}
