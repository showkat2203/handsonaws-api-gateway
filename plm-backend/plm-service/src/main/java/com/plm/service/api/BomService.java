package com.plm.service.api;

import com.plm.service.domain.BomNode;
import com.plm.service.domain.Part;

import java.util.List;

/**
 * BOM (Bill of Materials) hierarchy and where-used relationship traversal, backed by the graph
 * store. This is the single source of truth for structural/relationship queries — both the
 * GraphQL resolvers and the chatbot's tools call these same methods.
 */
public interface BomService {

    /** The full BOM tree rooted at {@code partId} (e.g. the servers/PSUs/NICs/cables that make up a rack). */
    BomNode getBom(String partId);

    /** Every assembly that directly or indirectly contains {@code partId} (e.g. "what uses PSU-2200?"). */
    List<Part> whereUsed(String partId);

    /** Adds/updates a "parent contains child x quantity" BOM link. Rejects cycles. */
    BomNode addBomComponent(String parentId, String childId, int quantity);

    void removeBomComponent(String parentId, String childId);
}
