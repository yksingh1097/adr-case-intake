package dev.adrintake.repository;

import dev.adrintake.model.DocumentDetail;
import dev.adrintake.model.DocumentSummary;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    List<DocumentSummary> findAll();

    Optional<DocumentDetail> findById(String id);
}