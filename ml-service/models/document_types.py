from enum import Enum

class DocumentType(str, Enum):
    """Типы документов"""
    INVOICE = "INVOICE"           # Счет на оплату
    ACT = "ACT"                   # Акт выполненных работ
    DELIVERY_NOTE = "DELIVERY_NOTE"  # Накладная
    CONTRACT = "CONTRACT"         # Договор
    UNKNOWN = "UNKNOWN"           # Неизвестный тип

# Ключевые слова для определения типа документа
KEYWORDS_MAP = {
    DocumentType.INVOICE: [
        "счет на оплату", "счет №", "invoice", "bill",
        "к оплате", "оплатите", "счет-фактура"
    ],
    DocumentType.ACT: [
        "акт выполненных работ", "акт об оказании услуг",
        "акт сдачи-приемки", "акт №", "выполненные работы"
    ],
    DocumentType.DELIVERY_NOTE: [
        "товарная накладная", "накладная №", "торг-12",
        "товарно-транспортная накладная", "отгрузка"
    ],
    DocumentType.CONTRACT: [
        "договор №", "контракт", "договор поставки",
        "договор оказания услуг", "соглашение"
    ]
}