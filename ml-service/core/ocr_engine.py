import pytesseract
import logging
from typing import Optional, Dict
from PIL import Image
import numpy as np
import re
import sys
import os

tesseract_path = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
if not os.path.exists(tesseract_path):
    # Попробуем другой распространённый путь
    tesseract_path = r"C:\Users\tatiana\AppData\Local\Programs\Tesseract-OCR\tesseract.exe"
    if not os.path.exists(tesseract_path):
        # Если не нашли, выведем ошибку, но попробуем стандартный поиск
        logging.warning("Tesseract not found at expected paths, will rely on PATH")
    else:
        pytesseract.pytesseract.tesseract_cmd = tesseract_path
else:
    pytesseract.pytesseract.tesseract_cmd = tesseract_path

# Остальной код класса OCREngine...

logger = logging.getLogger(__name__)

class OCREngine:
    """Класс для распознавания текста с изображений"""

    def __init__(self, config: Optional[str] = None):
        # Оптимизированная конфигурация для русского текста
        self.config = config or '--oem 3 --psm 6 -c tessedit_char_whitelist=абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789№.,:;/-()'
        self.languages = 'rus+eng'
        logger.info("OCR Engine initialized")

    def extract_text(self, image: np.ndarray) -> str:
        """Извлечение текста с улучшенной обработкой"""
        try:
            # Конвертация в PIL Image
            pil_image = Image.fromarray(image)

            # Пробуем несколько режимов PSM
            psm_modes = [6, 3, 4, 8, 12]
            best_text = ""
            best_confidence = 0

            for psm in psm_modes:
                config = f'--oem 3 --psm {psm} --dpi 300'
                try:
                    # Получаем данные с уверенностью
                    data = pytesseract.image_to_data(
                        pil_image,
                        lang=self.languages,
                        config=config,
                        output_type=pytesseract.Output.DICT
                    )

                    # Собираем текст и считаем уверенность
                    text_parts = []
                    total_conf = 0
                    conf_count = 0

                    for i, conf in enumerate(data['conf']):
                        if conf > 0 and data['text'][i].strip():
                            text_parts.append(data['text'][i])
                            total_conf += conf
                            conf_count += 1

                    if conf_count > 0:
                        avg_conf = total_conf / conf_count
                        full_text = ' '.join(text_parts)

                        if avg_conf > best_confidence:
                            best_confidence = avg_conf
                            best_text = full_text

                        logger.info("PSM %d: confidence %.2f, text length %d",
                                    psm, avg_conf, len(full_text))

                except Exception as e:
                    logger.warning("PSM %d failed: %s", psm, str(e))

            if not best_text:
                # Fallback: обычный вызов
                best_text = pytesseract.image_to_string(
                    pil_image,
                    lang=self.languages,
                    config='--oem 3 --psm 6'
                )

            # Очистка текста
            cleaned = self._clean_text(best_text)
            logger.info("OCR extracted %d characters (confidence: %.2f)",
                        len(cleaned), best_confidence)

            return cleaned

        except Exception as e:
            logger.error("OCR failed: %s", str(e), exc_info=True)
            return ""

    def _clean_text(self, text: str) -> str:
        """Очистка текста"""
        # Заменяем специфические символы
        text = text.replace('|', 'I').replace('|', '1')
        # Удаляем лишние пробелы
        text = re.sub(r'\s+', ' ', text)
        # Удаляем пустые строки
        lines = [line.strip() for line in text.split('\n') if line.strip()]
        return '\n'.join(lines)

    def image_to_string_debug(self, image: np.ndarray) -> Dict:
        """Отладочный метод для проверки"""
        pil_image = Image.fromarray(image)

        # Получаем детальные данные
        data = pytesseract.image_to_data(
            pil_image,
            lang=self.languages,
            config='--oem 3 --psm 6',
            output_type=pytesseract.Output.DICT
        )

        return {
            'text': data['text'],
            'conf': data['conf'],
            'left': data['left'],
            'top': data['top']
        }