package com.plm.service.api;

import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.NewPart;
import com.plm.service.domain.PageResult;
import com.plm.service.domain.Part;
import com.plm.service.domain.PartFilter;
import com.plm.service.domain.PartUpdate;

/**
 * CRUD and lifecycle operations on parts. This is the single source of truth for part business
 * logic — both the GraphQL resolvers and the chatbot's tools call these same methods.
 */
public interface PartService {

    Part getPart(String id);

    PageResult<Part> searchParts(PartFilter filter);

    Part createPart(NewPart newPart);

    /**
     * Creates a part with a caller-supplied id and initial lifecycle state, bypassing the
     * normal "always starts in DESIGN" rule. Intended for bulk-importing pre-existing inventory
     * (e.g. the seed data loader) rather than everyday part creation, so it requires ADMIN.
     */
    Part createPartWithId(String id, NewPart newPart, LifecycleState initialState);

    Part updatePart(String id, PartUpdate update);

    void deletePart(String id);

    /** Advances lifecycle state (design -&gt; active -&gt; eol); rejects backward transitions. */
    Part updateLifecycle(String id, LifecycleState newState);
}
