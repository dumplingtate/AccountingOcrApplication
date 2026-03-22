package com.accounting.ocr.model;

public enum DocumentType {
    INVOICE("Счет на оплату"),
    ACT("Акт выполненных работ"),
    DELIVERY_NOTE("Накладная"),
    CONTRACT("Договор"),
    UNKNOWN("Неизвестный тип");

    private final String description;

    DocumentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
