package com.accounting.ocr.service.ml.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MLRequest {
    private String fileId;
    private String fileName;
    private String filePath;
    private boolean needsPreprocessing;
}