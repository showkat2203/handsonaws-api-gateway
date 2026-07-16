package com.plm.service.repository.dynamo;

import com.plm.service.domain.ChangeOrder;

import java.util.List;
import java.util.Optional;

/** Change-order (ECO) store, backed by DynamoDB. Swappable for a local mock in tests. */
public interface ChangeOrderRepository {

    Optional<ChangeOrder> findById(String id);

    List<ChangeOrder> findAll();

    void save(ChangeOrder changeOrder);

    void deleteById(String id);
}
