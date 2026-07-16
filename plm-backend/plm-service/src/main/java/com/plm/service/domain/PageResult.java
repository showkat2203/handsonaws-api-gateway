package com.plm.service.domain;

import java.util.List;

/** A page of results plus paging metadata, returned by list/search service methods. */
public record PageResult<T>(List<T> items, long totalCount, int page, int size) {

    public PageResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, long totalCount, int page, int size) {
        return new PageResult<>(items, totalCount, page, size);
    }
}
