package com.plm.service.repository.dynamo;

import com.plm.service.domain.Part;

import java.util.List;
import java.util.Optional;

/** Part attributes/lifecycle status store, backed by DynamoDB. Swappable for a local mock in tests. */
public interface PartAttributeRepository {

    Optional<Part> findById(String id);

    /** Full unfiltered scan (small demo dataset) — used as the base set for in-memory filtering/paging. */
    List<Part> findAll();

    void save(Part part);

    void deleteById(String id);

    boolean existsById(String id);
}
