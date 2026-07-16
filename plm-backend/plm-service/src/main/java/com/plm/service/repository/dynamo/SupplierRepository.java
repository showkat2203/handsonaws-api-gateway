package com.plm.service.repository.dynamo;

import com.plm.service.domain.Supplier;

import java.util.List;
import java.util.Optional;

/** Supplier store, backed by DynamoDB. Swappable for a local mock in tests. */
public interface SupplierRepository {

    Optional<Supplier> findById(String id);

    List<Supplier> findAll();

    void save(Supplier supplier);

    void deleteById(String id);

    boolean existsById(String id);
}
