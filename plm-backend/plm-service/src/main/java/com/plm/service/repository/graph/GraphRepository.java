package com.plm.service.repository.graph;

import java.util.List;

/**
 * Structural relationships between parts: BOM hierarchy (parent "contains" child), where-used
 * (transitive ancestors), and supplier -> part links. Backed by a Gremlin-compatible graph
 * (TinkerGraph locally, Neptune in production) accessed exclusively through the TinkerPop
 * fluent {@code GraphTraversalSource} API — no raw Gremlin query strings.
 */
public interface GraphRepository {

    /** Idempotently ensures a "part" vertex exists for this id. */
    void upsertPartVertex(String partId);

    /** Idempotently ensures a "supplier" vertex exists for this id. */
    void upsertSupplierVertex(String supplierId);

    /** Removes the part vertex and every edge touching it (BOM edges, supplier links). */
    void deletePartVertex(String partId);

    boolean partVertexExists(String partId);

    /** Adds/updates a "contains" edge parent -> child with the given quantity. Both vertices must already exist. */
    void addBomEdge(String parentPartId, String childPartId, int quantity);

    /** Removes the "contains" edge parent -> child, if present. */
    void removeBomEdge(String parentPartId, String childPartId);

    /** Direct BOM children of a part (one level down), with quantities. */
    List<BomEdge> getDirectChildren(String parentPartId);

    /** Direct BOM parents of a part (one level up) — i.e. assemblies that directly contain it. */
    List<String> getDirectParents(String childPartId);

    /** All transitive ancestors of a part (every assembly that directly or indirectly contains it). */
    List<String> getWhereUsed(String partId);

    /** Links a supplier vertex to a part vertex ("suppliedBy" edge). Replaces any prior link. */
    void linkSupplierToPart(String supplierId, String partId);

    void unlinkSupplierFromPart(String partId);

    /** Ids of parts supplied by the given supplier. */
    List<String> getPartsBySupplier(String supplierId);

    /** Id of the supplier for a part, or null if unlinked. */
    String getSupplierForPart(String partId);
}
