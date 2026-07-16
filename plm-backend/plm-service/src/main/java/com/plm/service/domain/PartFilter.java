package com.plm.service.domain;

/**
 * Search filter for {@code PartService#searchParts}. All fields optional (null/blank = not filtered).
 * {@code page} is zero-based.
 */
public record PartFilter(String nameContains, PartType type, LifecycleState lifecycleState,
                          String supplierId, int page, int size) {

    public PartFilter {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 200) size = 200;
    }

    public static PartFilter empty() {
        return new PartFilter(null, null, null, null, 0, 20);
    }
}
