package com.plm.service.repository.dynamo;

import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.Part;
import com.plm.service.domain.PartType;
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
public class DynamoPartAttributeRepository implements PartAttributeRepository {

    private final DynamoDbTable<PartRecord> table;

    public DynamoPartAttributeRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("plm_parts", TableSchema.fromBean(PartRecord.class));
    }

    @Override
    public Optional<Part> findById(String id) {
        PartRecord record = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(record).map(DynamoPartAttributeRepository::toDomain);
    }

    @Override
    public List<Part> findAll() {
        return StreamSupport.stream(table.scan().items().spliterator(), false)
                .map(DynamoPartAttributeRepository::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Part part) {
        table.putItem(toRecord(part));
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
    }

    @Override
    public boolean existsById(String id) {
        return table.getItem(Key.builder().partitionValue(id).build()) != null;
    }

    private static PartRecord toRecord(Part part) {
        PartRecord r = new PartRecord();
        r.setId(part.getId());
        r.setName(part.getName());
        r.setDescription(part.getDescription());
        r.setType(part.getType().name());
        r.setLifecycleState(part.getLifecycleState().name());
        r.setRevision(part.getRevision());
        r.setSupplierId(part.getSupplierId());
        r.setAttributes(part.getAttributes());
        r.setCreatedAt(part.getCreatedAt() == null ? null : part.getCreatedAt().toString());
        r.setUpdatedAt(part.getUpdatedAt() == null ? null : part.getUpdatedAt().toString());
        return r;
    }

    private static Part toDomain(PartRecord r) {
        return new Part(
                r.getId(),
                r.getName(),
                r.getDescription(),
                PartType.valueOf(r.getType()),
                LifecycleState.valueOf(r.getLifecycleState()),
                r.getRevision(),
                r.getSupplierId(),
                r.getAttributes(),
                r.getCreatedAt() == null ? null : Instant.parse(r.getCreatedAt()),
                r.getUpdatedAt() == null ? null : Instant.parse(r.getUpdatedAt())
        );
    }
}
