package com.accounting.ocr.model;

public enum ProcessingStatus {
    UPLOADED("Загружен"),
    OCR_IN_PROGRESS("OCR в процессе"),
    ML_PROCESSING("ML обработка"),
    PROCESSED("Обработан"),
    VALIDATION_FAILED("Ошибка валидации"),
    PROCESSED_WITH_WARNINGS("Обработано с варнингами"),
    ERROR("Ошибка обработки");

    private final String description;

    ProcessingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
