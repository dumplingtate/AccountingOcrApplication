import cv2
import numpy as np
import logging

logger = logging.getLogger(__name__)

class ImagePreprocessor:
    """Класс для предобработки изображений перед OCR"""

    @staticmethod
    def preprocess(image_bytes: bytes) -> np.ndarray:
        """
        Полная предобработка изображения
        """
        # Конвертация байтов в OpenCV изображение
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if img is None:
            raise ValueError("Failed to decode image")

        logger.info("Original image size: %dx%d", img.shape[1], img.shape[0])

        # Сохраняем цветное изображение
        color_img = img.copy()

        # Преобразование в оттенки серого
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        # Пробуем разные методы и выбираем лучший
        results = []

        # Метод 1: Оригинал (просто серый)
        results.append(("original_gray", gray))

        # Метод 2: Увеличение контраста (простая нормализация)
        norm = cv2.normalize(gray, None, 0, 255, cv2.NORM_MINMAX)
        results.append(("normalized", norm))

        # Метод 3: Адаптивная бинаризация (только если текст на фоне)
        adaptive = cv2.adaptiveThreshold(
            gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 15, 2
        )
        results.append(("adaptive", adaptive))

        # Метод 4: Otsu бинаризация
        _, otsu = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        results.append(("otsu", otsu))

        # Выбираем метод с наибольшим количеством текстовых областей
        best_img = gray
        best_score = 0

        for name, img_result in results:
            # Оценка качества: количество краев (текст = много краев)
            edges = cv2.Canny(img_result, 50, 150)
            score = np.sum(edges > 0) / (img_result.shape[0] * img_result.shape[1])

            logger.info("Method %s score: %.4f", name, score)

            if score > best_score:
                best_score = score
                best_img = img_result

        logger.info("Best method selected with score: %.4f", best_score)

        # Дополнительное улучшение: удаление шума
        best_img = cv2.medianBlur(best_img, 3)

        # Увеличение размера с сохранением качества
        if best_img.shape[1] < 1000:
            scale = 1000 / best_img.shape[1]
            new_width = int(best_img.shape[1] * scale)
            new_height = int(best_img.shape[0] * scale)
            # Используем INTER_CUBIC для лучшего качества
            best_img = cv2.resize(best_img, (new_width, new_height), interpolation=cv2.INTER_CUBIC)
            # Лёгкое размытие для сглаживания артефактов
            best_img = cv2.GaussianBlur(best_img, (1, 1), 0)

        return best_img