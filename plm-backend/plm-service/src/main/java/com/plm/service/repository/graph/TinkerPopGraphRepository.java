package com.plm.service.repository.graph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.T;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link GraphRepository} implementation built entirely on the TinkerPop fluent
 * {@code GraphTraversalSource} API ({@code g.V()...}, {@code g.addV()...}). Works unchanged
 * against an embedded TinkerGraph, a local Gremlin Server, or Amazon Neptune, since none of
 * the traversal construction depends on the backing implementation.
 *
 * Graph model:
 * <ul>
 *   <li>vertices: label "part" (id = partId) and label "supplier" (id = supplierId)</li>
 *   <li>edges: "contains" parent-part -&gt; child-part, property "quantity"</li>
 *   <li>edges: "suppliedBy" part -&gt; supplier</li>
 * </ul>
 */
@Repository
public class TinkerPopGraphRepository implements GraphRepository {

    private static final String PART_LABEL = "part";
    private static final String SUPPLIER_LABEL = "supplier";
    private static final String CONTAINS = "contains";
    private static final String SUPPLIED_BY = "suppliedBy";
    private static final String QUANTITY = "quantity";

    private final GraphTraversalSource g;

    public TinkerPopGraphRepository(GraphTraversalSource g) {
        this.g = g;
    }

    @Override
    public synchronized void upsertPartVertex(String partId) {
        g.V(partId).fold()
                .coalesce(__.unfold(), __.addV(PART_LABEL).property(T.id, partId))
                .iterate();
    }

    @Override
    public synchronized void upsertSupplierVertex(String supplierId) {
        g.V(supplierId).fold()
                .coalesce(__.unfold(), __.addV(SUPPLIER_LABEL).property(T.id, supplierId))
                .iterate();
    }

    @Override
    public synchronized void deletePartVertex(String partId) {
        g.V(partId).drop().iterate();
    }

    @Override
    public boolean partVertexExists(String partId) {
        return g.V(partId).hasLabel(PART_LABEL).hasNext();
    }

    @Override
    public synchronized void addBomEdge(String parentPartId, String childPartId, int quantity) {
        g.V(parentPartId).outE(CONTAINS).where(__.inV().hasId(childPartId)).drop().iterate();
        g.V(parentPartId).as("p")
                .V(childPartId)
                .addE(CONTAINS).from("p")
                .property(QUANTITY, quantity)
                .iterate();
    }

    @Override
    public synchronized void removeBomEdge(String parentPartId, String childPartId) {
        g.V(parentPartId).outE(CONTAINS).where(__.inV().hasId(childPartId)).drop().iterate();
    }

    @Override
    public List<BomEdge> getDirectChildren(String parentPartId) {
        List<Map<String, Object>> rows = g.V(parentPartId).outE(CONTAINS)
                .project("childId", QUANTITY)
                .by(__.inV().id())
                .by(QUANTITY)
                .toList();
        return rows.stream()
                .map(row -> new BomEdge(String.valueOf(row.get("childId")), ((Number) row.get(QUANTITY)).intValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getDirectParents(String childPartId) {
        return g.V(childPartId).in(CONTAINS).id().toList()
                .stream().map(String::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<String> getWhereUsed(String partId) {
        return g.V(partId).repeat(__.in(CONTAINS)).emit().dedup().id().toList()
                .stream().map(String::valueOf).collect(Collectors.toList());
    }

    @Override
    public synchronized void linkSupplierToPart(String supplierId, String partId) {
        g.V(partId).outE(SUPPLIED_BY).drop().iterate();
        g.V(partId).as("p")
                .V(supplierId)
                .addE(SUPPLIED_BY).from("p")
                .iterate();
    }

    @Override
    public synchronized void unlinkSupplierFromPart(String partId) {
        g.V(partId).outE(SUPPLIED_BY).drop().iterate();
    }

    @Override
    public List<String> getPartsBySupplier(String supplierId) {
        return g.V(supplierId).in(SUPPLIED_BY).id().toList()
                .stream().map(String::valueOf).collect(Collectors.toList());
    }

    @Override
    public String getSupplierForPart(String partId) {
        Optional<Object> id = g.V(partId).out(SUPPLIED_BY).id().tryNext();
        return id.map(String::valueOf).orElse(null);
    }
}
