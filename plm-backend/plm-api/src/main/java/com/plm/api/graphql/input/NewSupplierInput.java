package com.plm.api.graphql.input;

public record NewSupplierInput(String name, String contactEmail, String address, Boolean active) {
}
