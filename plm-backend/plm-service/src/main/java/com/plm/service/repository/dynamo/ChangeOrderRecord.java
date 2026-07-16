package com.plm.service.repository.dynamo;

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.ArrayList;
import java.util.List;

/** DynamoDB item for an Engineering Change Order (table: {@code plm_change_orders}). */
@DynamoDbBean
public class ChangeOrderRecord {

    private String id;
    private String title;
    private String description;
    private String status;
    private List<String> affectedPartIds = new ArrayList<>();
    private String createdBy;
    private String createdAt;
    private String updatedAt;
    private String implementedAt;
    private Long version;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getAffectedPartIds() {
        return affectedPartIds;
    }

    public void setAffectedPartIds(List<String> affectedPartIds) {
        this.affectedPartIds = affectedPartIds;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getImplementedAt() {
        return implementedAt;
    }

    public void setImplementedAt(String implementedAt) {
        this.implementedAt = implementedAt;
    }

    @DynamoDbVersionAttribute
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
