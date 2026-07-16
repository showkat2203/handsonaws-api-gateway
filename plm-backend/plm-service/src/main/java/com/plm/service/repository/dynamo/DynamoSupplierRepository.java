package com.plm.service.repository.dynamo;

import com.plm.service.domain.Supplier;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
public class DynamoSupplierRepository implements SupplierRepository {

    private final DynamoDbTable<SupplierRecord> table;

    public DynamoSupplierRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("plm_suppliers", TableSchema.fromBean(SupplierRecord.class));
    }

    @Override
    public Optional<Supplier> findById(String id) {
        SupplierRecord record = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(record).map(DynamoSupplierRepository::toDomain);
    }

    @Override
    public List<Supplier> findAll() {
        return StreamSupport.stream(table.scan().items().spliterator(), false)
                .map(DynamoSupplierRepository::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Supplier supplier) {
        table.putItem(toRecord(supplier));
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
    }

    @Override
    public boolean existsById(String id) {
        return table.getItem(Key.builder().partitionValue(id).build()) != null;
    }

    private static SupplierRecord toRecord(Supplier s) {
        SupplierRecord r = new SupplierRecord();
        r.setId(s.getId());
        r.setName(s.getName());
        r.setContactEmail(s.getContactEmail());
        r.setAddress(s.getAddress());
        r.setActive(s.isActive());
        return r;
    }

    private static Supplier toDomain(SupplierRecord r) {
        return new Supplier(r.getId(), r.getName(), r.getContactEmail(), r.getAddress(),
                Boolean.TRUE.equals(r.getActive()));
    }
}
