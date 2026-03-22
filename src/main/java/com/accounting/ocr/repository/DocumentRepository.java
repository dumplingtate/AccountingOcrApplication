package com.accounting.ocr.repository;

import com.accounting.ocr.model.Document;
import com.accounting.ocr.model.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByFileId(String fileId);

    List<Document> findByStatus(ProcessingStatus status);

    List<Document> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatus(ProcessingStatus status);
}