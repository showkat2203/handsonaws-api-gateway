package com.plm.service.repository.graph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link TinkerPopGraphRepository} against a real embedded TinkerGraph (not a mock) so
 * the actual Gremlin traversals are verified, not just that methods were called.
 */
class TinkerPopGraphRepositoryTest {

    private TinkerPopGraphRepository repository;

    @BeforeEach
    void setUp() {
        GraphTraversalSource g = GremlinConfig.embeddedTraversalSource();
        repository = new TinkerPopGraphRepository(g);
    }

    @Test
    void bomHierarchy_roundTrips() {
        repository.upsertPartVertex("RACK-1");
        repository.upsertPartVertex("SERVER-1");
        repository.upsertPartVertex("PSU-1");

        repository.addBomEdge("RACK-1", "SERVER-1", 4);
        repository.addBomEdge("SERVER-1", "PSU-1", 2);

        List<BomEdge> rackChildren = repository.getDirectChildren("RACK-1");
        assertThat(rackChildren).containsExactly(new BomEdge("SERVER-1", 4));

        List<String> psuAncestors = repository.getWhereUsed("PSU-1");
        assertThat(psuAncestors).containsExactlyInAnyOrder("SERVER-1", "RACK-1");

        List<String> serverParents = repository.getDirectParents("SERVER-1");
        assertThat(serverParents).containsExactly("RACK-1");
    }

    @Test
    void addBomEdge_isIdempotentAndUpdatesQuantity() {
        repository.upsertPartVertex("A");
        repository.upsertPartVertex("B");

        repository.addBomEdge("A", "B", 1);
        repository.addBomEdge("A", "B", 5);

        List<BomEdge> children = repository.getDirectChildren("A");
        assertThat(children).hasSize(1);
        assertThat(children.get(0).quantity()).isEqualTo(5);
    }

    @Test
    void removeBomEdge_removesLink() {
        repository.upsertPartVertex("A");
        repository.upsertPartVertex("B");
        repository.addBomEdge("A", "B", 1);

        repository.removeBomEdge("A", "B");

        assertThat(repository.getDirectChildren("A")).isEmpty();
    }

    @Test
    void deletePartVertex_removesIncidentEdges() {
        repository.upsertPartVertex("A");
        repository.upsertPartVertex("B");
        repository.addBomEdge("A", "B", 1);

        repository.deletePartVertex("B");

        assertThat(repository.partVertexExists("B")).isFalse();
        assertThat(repository.getDirectChildren("A")).isEmpty();
    }

    @Test
    void supplierLinks_roundTrip() {
        repository.upsertPartVertex("PART-1");
        repository.upsertSupplierVertex("SUP-1");
        repository.upsertSupplierVertex("SUP-2");

        repository.linkSupplierToPart("SUP-1", "PART-1");
        assertThat(repository.getSupplierForPart("PART-1")).isEqualTo("SUP-1");
        assertThat(repository.getPartsBySupplier("SUP-1")).containsExactly("PART-1");

        repository.linkSupplierToPart("SUP-2", "PART-1");
        assertThat(repository.getSupplierForPart("PART-1")).isEqualTo("SUP-2");
        assertThat(repository.getPartsBySupplier("SUP-1")).isEmpty();
    }

    @Test
    void whereUsed_isTransitiveAcrossMultipleLevels() {
        repository.upsertPartVertex("RACK-1");
        repository.upsertPartVertex("SERVER-1");
        repository.upsertPartVertex("PSU-1");
        repository.addBomEdge("RACK-1", "SERVER-1", 4);
        repository.addBomEdge("SERVER-1", "PSU-1", 2);

        assertThat(repository.getWhereUsed("PSU-1")).containsExactlyInAnyOrder("SERVER-1", "RACK-1");
        assertThat(repository.getWhereUsed("RACK-1")).isEmpty();
    }
}
