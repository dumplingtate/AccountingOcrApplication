package com.accounting.ocr.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadDocumentRequest {
    private MultipartFile file;
    private String description;
}