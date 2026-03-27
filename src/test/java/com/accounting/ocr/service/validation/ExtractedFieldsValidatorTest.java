package com.accounting.ocr.service.validation;

import com.accounting.ocr.dto.ValidationError;
import com.accounting.ocr.model.DocumentType;
import com.accounting.ocr.service.ml.dto.MLResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtractedFieldsValidatorTest {

    private ExtractedFieldsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ExtractedFieldsValidator();
    }

    @Test
    void validate_WithValidData_ShouldReturnNoErrors() {
        MLResponse response = MLResponse.builder()
                .documentType(DocumentType.INVOICE)
                .confidence(0.95)
                .documentNumber("INV-001")
                .documentDate(LocalDate.now().minusDays(1))
                .totalAmount(new BigDecimal("15000.00"))
                .counterpartyInn("7701234567")
                .build();

        List<ValidationError> errors = validator.validate(response);

        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_WithMissingRequiredFields_ShouldReturnErrors() {
        MLResponse response = MLResponse.builder()
                .documentType(DocumentType.UNKNOWN)
                .confidence(0.4)
                .build();

        List<ValidationError> errors = validator.validate(response);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("documentNumber")));
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("documentDate")));
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("totalAmount")));
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("counterpartyInn")));
    }

    @Test
    void validate_WithInvalidINN_ShouldReturnError() {
        MLResponse response = MLResponse.builder()
                .documentType(DocumentType.INVOICE)
                .confidence(0.95)
                .documentNumber("INV-001")
                .documentDate(LocalDate.now())
                .totalAmount(new BigDecimal("15000.00"))
                .counterpartyInn("123")  // Invalid INN
                .build();

        List<ValidationError> errors = validator.validate(response);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("counterpartyInn")));
    }

    @Test
    void validate_WithFutureDate_ShouldReturnError() {
        MLResponse response = MLResponse.builder()
                .documentType(DocumentType.INVOICE)
                .confidence(0.95)
                .documentNumber("INV-001")
                .documentDate(LocalDate.now().plusDays(10))
                .totalAmount(new BigDecimal("15000.00"))
                .counterpartyInn("7701234567")
                .build();

        List<ValidationError> errors = validator.validate(response);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("documentDate")));
    }

    @Test
    void validate_WithZeroAmount_ShouldReturnError() {
        MLResponse response = MLResponse.builder()
                .documentType(DocumentType.INVOICE)
                .confidence(0.95)
                .documentNumber("INV-001")
                .documentDate(LocalDate.now())
                .totalAmount(BigDecimal.ZERO)
                .counterpartyInn("7701234567")
                .build();

        List<ValidationError> errors = validator.validate(response);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("totalAmount")));
    }

    @Test
    void isDataSufficient_WithCompleteData_ShouldReturnTrue() {
        MLResponse response = MLResponse.builder()
                .documentNumber("INV-001")
                .documentDate(LocalDate.now())
                .totalAmount(new BigDecimal("15000.00"))
                .counterpartyInn("7701234567")
                .build();

        assertTrue(validator.isDataSufficient(response));
    }

    @Test
    void isDataSufficient_WithMissingData_ShouldReturnFalse() {
        MLResponse response = MLResponse.builder()
                .documentNumber("INV-001")
                .build();

        assertFalse(validator.isDataSufficient(response));
    }
}