import re
from typing import Tuple
import logging
from models.document_types import DocumentType, KEYWORDS_MAP

logger = logging.getLogger(__name__)


class DocumentClassifier:
    """Класс для классификации типов документов"""

    def __init__(self):
        self.keywords_map = KEYWORDS_MAP
        logger.info("DocumentClassifier initialized")

    def classify(self, text: str) -> Tuple[DocumentType, float]:
        """Классификация документа на основе текста"""
        text_lower = text.lower()

        scores = {}
        for doc_type, keywords in self.keywords_map.items():
            score = self._calculate_score(text_lower, keywords)
            scores[doc_type] = score

        best_type = max(scores, key=scores.get)
        best_score = scores[best_type]

        # Нормализуем confidence в диапазон [0.5, 0.95] для реальных документов
        if best_score >= 4:
            confidence = min(0.95, 0.6 + (best_score - 4) / 10)
        elif best_score >= 2:
            confidence = 0.5 + (best_score - 2) / 20
        else:
            confidence = max(0.2, best_score / 10)

        # Если документ содержит слова "счет на оплату", уверенность высокая
        if 'счет на оплату' in text_lower:
            confidence = max(confidence, 0.8)

        logger.info("Document classified as %s with confidence %.2f (score=%d)",
                    best_type, confidence, best_score)
        return best_type, confidence


    def _calculate_score(self, text: str, keywords: list) -> float:
        """Расчет score для типа документа"""
        score = 0.0
        for keyword in keywords:
            # Точное совпадение
            if keyword in text:
                score += 2.0
            # Слово в составе фразы
            elif re.search(rf'\b{re.escape(keyword)}\b', text):
                score += 1.0

        return score

    def classify_with_ml(self, text: str) -> Tuple[DocumentType, float]:
        """
        Классификация с использованием TF-IDF и классификатора
        (заготовка для будущего ML улучшения)
        """
        # Здесь можно интегрировать scikit-learn модель
        # Для текущей версии используем rule-based классификацию
        return self.classify(text)