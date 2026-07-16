// Binds the "g" traversal source used by plm-service's GraphTraversalSource client, against the
// empty TinkerGraph loaded per gremlin-server.yaml's `graphs` block. Vertex/edge ids are
// configured as String (ANY id manager, see tinkergraph-empty.properties) to match the
// part/supplier ids the application uses.
g = graph.traversal()
