package com.plm.service.api;

import com.plm.service.domain.ChangeOrder;
import com.plm.service.domain.ChangeOrderFilter;
import com.plm.service.domain.NewChangeOrder;
import com.plm.service.domain.PageResult;

/**
 * CRUD and workflow transitions (draft -&gt; submitted -&gt; approved/rejected -&gt; implemented)
 * for Engineering Change Orders. This is the single source of truth for ECO business logic —
 * both the GraphQL resolvers and the chatbot's tools call these same methods.
 */
public interface ChangeOrderService {

    ChangeOrder getChangeOrder(String id);

    PageResult<ChangeOrder> changeOrders(ChangeOrderFilter filter);

    ChangeOrder createChangeOrder(NewChangeOrder newChangeOrder, String createdBy);

    /** Only allowed while status is DRAFT. */
    ChangeOrder updateChangeOrder(String id, NewChangeOrder update);

    ChangeOrder submitChangeOrder(String id);

    ChangeOrder approveChangeOrder(String id);

    ChangeOrder rejectChangeOrder(String id);

    ChangeOrder implementChangeOrder(String id);

    /** Only allowed while status is DRAFT or REJECTED (approved/implemented history is retained). */
    void deleteChangeOrder(String id);
}
