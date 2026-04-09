from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import Optional, Dict, Any
import logging
import uuid
import time
import io

from core.image_preprocessor import ImagePreprocessor
from core.ocr_engine import OCREngine
from core.document_classifier import DocumentClassifier
from core.field_extractor import FieldExtractor
from models.document_types import DocumentType

# Настройка логирования
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Создание FastAPI приложения
app = FastAPI(
    title="Document OCR Service",
    description="Service for document recognition and classification",
    version="1.0.0"
)

# Инициализация компонентов
image_preprocessor = ImagePreprocessor()
ocr_engine = OCREngine()
document_classifier = DocumentClassifier()
field_extractor = FieldExtractor()


class ClassificationResponse(BaseModel):
    """Ответ от ML сервиса"""
    document_type: DocumentType
    confidence: float
    document_number: Optional[str] = None
    document_date: Optional[str] = None
    total_amount: Optional[float] = None
    vat_amount: Optional[float] = None
    counterparty_name: Optional[str] = None
    counterparty_inn: Optional[str] = None
    counterparty_kpp: Optional[str] = None
    additional_fields: Dict[str, Any] = Field(default_factory=dict)
    raw_text: Optional[str] = None
    processing_time: str


@app.get("/health")
async def health_check():
    """Проверка работоспособности сервиса"""
    return {"status": "healthy", "service": "document-ocr"}


# ml-service/app.py - обновленная версия с отладкой

@app.post("/classify", response_model=ClassificationResponse)
async def classify_document(
        file: UploadFile = File(...),
        file_id: Optional[str] = None,
        return_full_text: bool = False,
        debug: bool = False
):
    start_time = time.time()

    try:
        # Чтение файла
        contents = await file.read()
        logger.info("Received file: %s, size: %d bytes, type: %s",
                    file.filename, len(contents), file.content_type)

        # Сохраняем копию для отладки
        if debug:
            import os
            debug_dir = "debug_images"
            os.makedirs(debug_dir, exist_ok=True)
            with open(f"{debug_dir}/{file_id or 'debug'}_original.png", "wb") as f:
                f.write(contents)
            logger.info("Saved original image to debug folder")

        # Предобработка
        processed_image = image_preprocessor.preprocess(contents)

        if debug:
            import os
            import cv2
            debug_dir = "debug_images"
            cv2.imwrite(f"{debug_dir}/{file_id or 'debug'}_processed.png", processed_image)
            logger.info("Saved processed image to debug folder")

        # OCR
        extracted_text = ocr_engine.extract_text(processed_image)

        if debug:
            logger.info("=== OCR DEBUG OUTPUT ===")
            logger.info(extracted_text)
            logger.info("=========================")

        if not extracted_text or len(extracted_text.strip()) < 10:
            logger.warning("No text extracted or text too short")
            # Возвращаем отладочную информацию
            return ClassificationResponse(
                document_type=DocumentType.UNKNOWN,
                confidence=0.0,
                raw_text=extracted_text if return_full_text else None,
                processing_time=f"{time.time() - start_time:.2f}s",
                additional_fields={"debug": "No text extracted"}
            )

        # Классификация
        doc_type, confidence = document_classifier.classify(extracted_text)

        # Извлечение полей
        extracted_fields = field_extractor.extract_all_fields(extracted_text, doc_type)

        return ClassificationResponse(
            document_type=doc_type,
            confidence=confidence,
            document_number=extracted_fields.get('document_number'),
            document_date=extracted_fields.get('date'),
            total_amount=extracted_fields.get('total_amount'),
            vat_amount=extracted_fields.get('vat_amount'),
            counterparty_name=extracted_fields.get('counterparty_name'),
            counterparty_inn=extracted_fields.get('counterparty_inn'),
            counterparty_kpp=extracted_fields.get('counterparty_kpp'),
            raw_text=extracted_text if return_full_text else None,
            processing_time=f"{time.time() - start_time:.2f}s"
        )

    except Exception as e:
        logger.error("Error: %s", str(e), exc_info=True)
        raise HTTPException(status_code=500, detail=f"Processing error: {str(e)}")

@app.post("/classify/batch")
async def classify_batch(files: list[UploadFile] = File(...)):
    """
    Пакетная обработка документов
    """
    results = []
    for file in files:
        try:
            # Перематываем файл в начало
            await file.seek(0)
            result = await classify_document(file, return_full_text=False)
            results.append({
                "filename": file.filename,
                "result": result.dict(),
                "status": "success"
            })
        except Exception as e:
            results.append({
                "filename": file.filename,
                "error": str(e),
                "status": "failed"
            })

    return {"total": len(files), "results": results}


@app.get("/stats")
async def get_stats():
    """Статистика сервиса"""
    return {
        "status": "running",
        "components": {
            "ocr": "tesseract",
            "preprocessor": "opencv",
            "classifier": "rule-based"
        }
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=5001)