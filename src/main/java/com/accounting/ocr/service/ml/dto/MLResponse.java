package com.accounting.ocr.service.ml.dto;

import com.accounting.ocr.model.DocumentType;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("document_type")
    private DocumentType documentType;
    private Double confidence;
    @JsonProperty("document_number")
    private String documentNumber;
    @JsonProperty("document_date")
    private LocalDate documentDate;
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
    @JsonProperty("vat_amount")
    private BigDecimal vatAmount;

    @JsonProperty("counterparty_name")
    private String counterpartyName;
    @JsonProperty("counterparty_inn")
    private String counterpartyInn;
    @JsonProperty("counterparty_kpp")
    private String counterpartyKpp;

    @JsonProperty("additional_fields")
    private Map<String, String> additionalFields;

    @JsonProperty("raw_text")
    private String rawText;
    @JsonProperty("processing_time")
    private String processingTime;

    public boolean isValid() {
        return confidence != null && confidence > 0.5;
    }
}
