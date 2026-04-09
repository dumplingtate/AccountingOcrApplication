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

        // Не блокируем, если тип не определён — просто предупреждение
        if (mlResponse.getDocumentType() == null) {
            errors.add(new ValidationError("documentType", "Document type not determined (will be set to UNKNOWN)"));
        }

        // Не блокируем из-за низкой confidence
        if (mlResponse.getConfidence() != null && mlResponse.getConfidence() < 0.6) {
            errors.add(new ValidationError("confidence", "Low recognition confidence: " + mlResponse.getConfidence()));
        }

        // Номер документа — не обязателен
        if (mlResponse.getDocumentNumber() == null || mlResponse.getDocumentNumber().isEmpty()) {
            errors.add(new ValidationError("documentNumber", "Document number missing (will be set to 'UNKNOWN')"));
        }

        // Дата — не обязательна
        if (mlResponse.getDocumentDate() == null) {
            errors.add(new ValidationError("documentDate", "Document date missing (will be set to current date)"));
        } else if (mlResponse.getDocumentDate().isAfter(LocalDate.now())) {
            errors.add(new ValidationError("documentDate", "Future date, but will be accepted"));
        }

        // Сумма — не обязательна
        if (mlResponse.getTotalAmount() == null) {
            errors.add(new ValidationError("totalAmount", "Amount missing (will be set to 0)"));
        } else if (mlResponse.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationError("totalAmount", "Amount is zero or negative, but will be accepted"));
        }

        // ИНН — не обязателен
        if (mlResponse.getCounterpartyInn() == null || mlResponse.getCounterpartyInn().isEmpty()) {
            errors.add(new ValidationError("counterpartyInn", "INN missing (counterparty will be created without INN)"));
        }

        // Возвращаем список ошибок, но не бросаем исключение
        return errors;
    }

    public boolean isDataSufficient(MLResponse mlResponse) {
        return mlResponse.getDocumentNumber() != null && !mlResponse.getDocumentNumber().isEmpty()
                && mlResponse.getDocumentDate() != null
                && mlResponse.getTotalAmount() != null
                && mlResponse.getCounterpartyInn() != null && !mlResponse.getCounterpartyInn().isEmpty();
    }
}