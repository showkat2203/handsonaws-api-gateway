package com.plm.service.repository.dynamo;

import com.plm.service.domain.ChangeOrder;
import com.plm.service.domain.ChangeOrderStatus;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
public class DynamoChangeOrderRepository implements ChangeOrderRepository {

    private final DynamoDbTable<ChangeOrderRecord> table;

    public DynamoChangeOrderRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("plm_change_orders", TableSchema.fromBean(ChangeOrderRecord.class));
    }

    @Override
    public Optional<ChangeOrder> findById(String id) {
        ChangeOrderRecord record = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(record).map(DynamoChangeOrderRepository::toDomain);
    }

    @Override
    public List<ChangeOrder> findAll() {
        return StreamSupport.stream(table.scan().items().spliterator(), false)
                .map(DynamoChangeOrderRepository::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void save(ChangeOrder changeOrder) {
        table.putItem(toRecord(changeOrder));
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
    }

    private static ChangeOrderRecord toRecord(ChangeOrder co) {
        ChangeOrderRecord r = new ChangeOrderRecord();
        r.setId(co.getId());
        r.setTitle(co.getTitle());
        r.setDescription(co.getDescription());
        r.setStatus(co.getStatus().name());
        r.setAffectedPartIds(co.getAffectedPartIds());
        r.setCreatedBy(co.getCreatedBy());
        r.setCreatedAt(co.getCreatedAt() == null ? null : co.getCreatedAt().toString());
        r.setUpdatedAt(co.getUpdatedAt() == null ? null : co.getUpdatedAt().toString());
        r.setImplementedAt(co.getImplementedAt() == null ? null : co.getImplementedAt().toString());
        return r;
    }

    private static ChangeOrder toDomain(ChangeOrderRecord r) {
        return new ChangeOrder(
                r.getId(), r.getTitle(), r.getDescription(), ChangeOrderStatus.valueOf(r.getStatus()),
                r.getAffectedPartIds(), r.getCreatedBy(),
                r.getCreatedAt() == null ? null : Instant.parse(r.getCreatedAt()),
                r.getUpdatedAt() == null ? null : Instant.parse(r.getUpdatedAt()),
                r.getImplementedAt() == null ? null : Instant.parse(r.getImplementedAt())
        );
    }
}
