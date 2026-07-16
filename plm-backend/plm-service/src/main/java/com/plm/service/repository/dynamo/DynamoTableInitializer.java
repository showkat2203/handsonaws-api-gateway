package com.plm.service.repository.dynamo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

/**
 * Creates the DynamoDB tables on startup if they don't already exist. Safe against a real AWS
 * account (a no-op there in practice since tables are provisioned by infra-as-code), but makes
 * DynamoDB Local usable with zero manual setup for local dev / tests.
 */
@Component
@Order(0)
public class DynamoTableInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DynamoTableInitializer.class);

    private final DynamoDbTable<PartRecord> partTable;
    private final DynamoDbTable<SupplierRecord> supplierTable;
    private final DynamoDbTable<ChangeOrderRecord> changeOrderTable;

    public DynamoTableInitializer(DynamoDbEnhancedClient enhancedClient) {
        this.partTable = enhancedClient.table("plm_parts", software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean(PartRecord.class));
        this.supplierTable = enhancedClient.table("plm_suppliers", software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean(SupplierRecord.class));
        this.changeOrderTable = enhancedClient.table("plm_change_orders", software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean(ChangeOrderRecord.class));
    }

    @Override
    public void afterPropertiesSet() {
        createIfMissing(partTable, "plm_parts");
        createIfMissing(supplierTable, "plm_suppliers");
        createIfMissing(changeOrderTable, "plm_change_orders");
    }

    private void createIfMissing(DynamoDbTable<?> table, String name) {
        try {
            table.createTable();
            log.info("Created DynamoDB table {}", name);
        } catch (ResourceInUseException e) {
            log.debug("DynamoDB table {} already exists", name);
        }
    }
}
