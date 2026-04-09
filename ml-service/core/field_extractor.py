import re
from typing import Dict, Optional
from datetime import datetime
import logging
from models.document_types import DocumentType

logger = logging.getLogger(__name__)

class FieldExtractor:
    """Улучшенный экстрактор полей – поддерживает переносы строк в дате, ищет покупателя, обрабатывает НДС"""

    PRIORITY_PATTERNS = {
        'document_number': [
            r'Счет\s+на\s+оплату\s*№\s*(\d+)',
            r'Счет\s*№\s*(\d+)',
            r'№\s*(\d+)\s+от',
        ],
        'document_date': [
            # Вариант с переносом строки внутри года (202\n3)
            r'от\s+(\d{1,2})\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря)\s+(\d{3})\s+(\d{1})\s*г\.?',
            # Обычный вариант
            r'от\s+(\d{1,2})\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря)\s+(\d{4})\s*г\.?',
        ],
        'counterparty_name': [
            # Покупатель – именно его мы считаем контрагентом в счете
            r'Покупатель\.?\s*([А-Яа-я\s\.]+(?:ООО|ЗАО|ОАО|ПАО|ИП|Общество|Компания)[^,\n]+)',
            r'Покупатель[:\n]\s*([А-Яа-я\s\.]+(?:ООО|ЗАО|ОАО|ПАО|ИП|Общество|Компания)[^,\n]+)',
        ],
        'counterparty_inn': [
            r'Покупатель.*?ИНН\s+(\d{10,12})',
            r',\s*ИНН\s+(\d{10,12})',
        ],
        'counterparty_kpp': [
            r'КПП\s*\([^)]*\)\s*:\s*(\d{9})',
            r'КПП\s*:\s*(\d{9})',
            r'КПП\s+(\d{9})',
        ],
    }

    GENERAL_PATTERNS = {
        'total_amount': [
            r'Итого[:]?\s*([\d\s]+[.,]\d{2})',
            r'Всего к оплате[:]?\s*([\d\s]+[.,]\d{2})',
        ],
        'vat_amount': [
            r'НДС\s*[:]?\s*([\d\s]+[.,]\d{2})',
            # Фраза "Без налога (НДС)" означает 0
            (r'Без налога \(НДС\)', 0.0),
        ],
    }

    def extract_all_fields(self, text: str, doc_type: DocumentType) -> Dict:
        # Убираем переносы строк – они мешают регулярным выражениям для даты
        text = re.sub(r'\n', ' ', text)
        fields = {}

        # Приоритетные паттерны
        for field_name, patterns in self.PRIORITY_PATTERNS.items():
            value = self._extract_field(text, patterns, field_name)
            if value is not None:
                fields[field_name] = value

        # Общие паттерны
        for field_name, patterns in self.GENERAL_PATTERNS.items():
            if field_name not in fields:
                value = self._extract_field(text, patterns, field_name)
                if value is not None:
                    fields[field_name] = value

        fields = self._post_process_fields(fields, text)

        # Если ИНН покупателя всё ещё не найден – пробуем последний резервный поиск
        if 'counterparty_inn' not in fields:
            match = re.search(r'Общество.*?ИНН\s+(\d{10,12})', text, re.DOTALL)
            if match:
                fields['counterparty_inn'] = match.group(1)

        logger.info("Extracted fields: %s", list(fields.keys()))
        return fields

    def _extract_field(self, text: str, patterns: list, field_name: str = None):
        for pattern in patterns:
            # Обработка паттерна-кортежа (регулярка, значение по умолчанию)
            if isinstance(pattern, tuple):
                regex, default_value = pattern
                if re.search(regex, text, re.IGNORECASE | re.MULTILINE | re.DOTALL):
                    return default_value
                continue

            match = re.search(pattern, text, re.IGNORECASE | re.MULTILINE | re.DOTALL)
            if not match:
                continue

            # Для даты с разрывом года (4 группы)
            if field_name == 'document_date' and len(match.groups()) == 4:
                day = match.group(1)
                month = match.group(2)
                year_part1 = match.group(3)
                year_part2 = match.group(4)
                year = year_part1 + year_part2
                return f"{day} {month} {year}"

            if match.groups():
                return match.group(1).strip()
            else:
                return True
        return None

    def _post_process_fields(self, fields: Dict, full_text: str) -> Dict:
        processed = {}

        if 'document_number' in fields:
            num = fields['document_number']
            if len(num) > 4:
                match = re.search(r'Счет\s+на\s+оплату\s*№?\s*(\d{1,4})', full_text, re.IGNORECASE)
                if match:
                    num = match.group(1)
            processed['document_number'] = num

        if 'document_date' in fields:
            date_str = fields['document_date']
            if re.search(r'\d{1,2}\s+[а-я]+\s+\d{4}', date_str, re.IGNORECASE):
                date_str = self._convert_russian_date(date_str)
            processed['document_date'] = self._parse_date(date_str)

        if 'total_amount' in fields:
            processed['total_amount'] = self._parse_amount(fields['total_amount'])
        if 'vat_amount' in fields:
            if fields['vat_amount'] == 0.0:
                processed['vat_amount'] = 0.0
            else:
                processed['vat_amount'] = self._parse_amount(fields['vat_amount'])

        if 'counterparty_inn' in fields:
            inn = re.sub(r'\D', '', fields['counterparty_inn'])
            if len(inn) in (10, 12):
                processed['counterparty_inn'] = inn
        if 'counterparty_kpp' in fields:
            kpp = re.sub(r'\D', '', fields['counterparty_kpp'])
            if len(kpp) == 9:
                processed['counterparty_kpp'] = kpp

        if 'counterparty_name' in fields:
            name = fields['counterparty_name'].strip()
            name = re.sub(r'\s+', ' ', name)
            if len(name) > 100:
                name = name[:100]
            processed['counterparty_name'] = name

        return processed

    def _convert_russian_date(self, date_str: str) -> str:
        months = {
            'января': '01', 'февраля': '02', 'марта': '03', 'апреля': '04',
            'мая': '05', 'июня': '06', 'июля': '07', 'августа': '08',
            'сентября': '09', 'октября': '10', 'ноября': '11', 'декабря': '12'
        }
        match = re.match(r'(\d{1,2})\s+([а-я]+)\s+(\d{3,4})', date_str, re.IGNORECASE)
        if not match:
            match = re.match(r'(\d{1,2})\s+([а-я]+)\s+(\d{3})\s+(\d{1})', date_str, re.IGNORECASE)
            if match:
                year = match.group(3) + match.group(4)
                day = match.group(1).zfill(2)
                month_name = match.group(2).lower()
                month = months.get(month_name, '01')
                return f"{day}.{month}.{year}"
        if match:
            day = match.group(1).zfill(2)
            month_name = match.group(2).lower()
            year = match.group(3)
            month = months.get(month_name, '01')
            return f"{day}.{month}.{year}"
        return date_str

    def _parse_date(self, date_str: str) -> Optional[str]:
        if not date_str:
            return None
        formats = ['%d.%m.%Y', '%Y-%m-%d', '%d/%m/%Y', '%d.%m.%y']
        for fmt in formats:
            try:
                date = datetime.strptime(date_str.strip(), fmt)
                return date.strftime('%Y-%m-%d')
            except ValueError:
                continue
        return None

    def _parse_amount(self, amount_str: str) -> Optional[float]:
        if not amount_str:
            return None
        amount_str = amount_str.replace(' ', '').replace(',', '.')
        match = re.search(r'(\d+(?:\.\d{2})?)', amount_str)
        if match:
            return float(match.group(1))
        return None