package com.plm.service.repository.graph;

/** A BOM parent/child relationship edge as stored in the graph, with the "uses" quantity. */
public record BomEdge(String partId, int quantity) {
}
