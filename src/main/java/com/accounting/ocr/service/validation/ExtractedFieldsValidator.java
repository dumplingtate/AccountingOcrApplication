package com.accounting.ocr.service.validation;

import com.accounting.ocr.service.ml.dto.MLResponse;
import com.accounting.ocr.dto.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ExtractedFieldsValidator {

    private static final Pattern INN_PATTERN = Pattern.compile("^\\d{10}$|^\\d{12}$");
    private static final Pattern DOCUMENT_NUMBER_PATTERN = Pattern.compile("^[А-Яа-яA-Za-z0-9№\\-\\/]{1,50}$");

    public List<ValidationError> validate(MLResponse mlResponse) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate document type
        if (mlResponse.getDocumentType() == null) {
            errors.add(new ValidationError("documentType", "Document type could not be determined"));
        }

        // Validate confidence
        if (mlResponse.getConfidence() != null && mlResponse.getConfidence() < 0.6) {
            errors.add(new ValidationError("confidence",
                    String.format("Low recognition confidence: %.2f", mlResponse.getConfidence())));
        }

        // Validate document number
        if (mlResponse.getDocumentNumber() != null && !mlResponse.getDocumentNumber().isEmpty()) {
            if (!DOCUMENT_NUMBER_PATTERN.matcher(mlResponse.getDocumentNumber()).matches()) {
                errors.add(new ValidationError("documentNumber",
                        "Document number contains invalid characters"));
            }
        } else {
            errors.add(new ValidationError("documentNumber", "Document number is missing"));
        }

        // Validate date
        if (mlResponse.getDocumentDate() != null) {
            LocalDate now = LocalDate.now();
            if (mlResponse.getDocumentDate().isAfter(now)) {
                errors.add(new ValidationError("documentDate", "Document date cannot be in the future"));
            }
            if (mlResponse.getDocumentDate().isBefore(now.minusYears(5))) {
                errors.add(new ValidationError("documentDate", "Document is too old (>5 years)"));
            }
        } else {
            errors.add(new ValidationError("documentDate", "Document date is missing"));
        }

        // Validate amount
        if (mlResponse.getTotalAmount() != null) {
            if (mlResponse.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new ValidationError("totalAmount", "Amount must be greater than zero"));
            }
            if (mlResponse.getTotalAmount().compareTo(new BigDecimal("999999999.99")) > 0) {
                errors.add(new ValidationError("totalAmount", "Amount exceeds maximum allowed value"));
            }
        } else {
            errors.add(new ValidationError("totalAmount", "Amount is missing"));
        }

        // Validate INN
        if (mlResponse.getCounterpartyInn() != null && !mlResponse.getCounterpartyInn().isEmpty()) {
            if (!INN_PATTERN.matcher(mlResponse.getCounterpartyInn()).matches()) {
                errors.add(new ValidationError("counterpartyInn",
                        "INN must be 10 or 12 digits"));
            }
        } else {
            errors.add(new ValidationError("counterpartyInn", "Counterparty INN is missing"));
        }

        log.info("Validation completed. Found {} errors", errors.size());
        return errors;
    }

    public boolean isDataSufficient(MLResponse mlResponse) {
        return mlResponse.getDocumentNumber() != null && !mlResponse.getDocumentNumber().isEmpty()
                && mlResponse.getDocumentDate() != null
                && mlResponse.getTotalAmount() != null
                && mlResponse.getCounterpartyInn() != null && !mlResponse.getCounterpartyInn().isEmpty();
    }
}