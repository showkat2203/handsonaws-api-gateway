package com.plm.service.domain;

/** Search filter for {@code ChangeOrderService#changeOrders}. Zero-based page. */
public record ChangeOrderFilter(ChangeOrderStatus status, String partId, int page, int size) {

    public ChangeOrderFilter {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 200) size = 200;
    }

    public static ChangeOrderFilter empty() {
        return new ChangeOrderFilter(null, null, 0, 20);
    }
}
