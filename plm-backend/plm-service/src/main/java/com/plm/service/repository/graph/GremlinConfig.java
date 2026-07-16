package com.plm.service.repository.graph;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Produces the {@link GraphTraversalSource} ("g") used by {@link TinkerPopGraphRepository}.
 * All Gremlin access in this codebase goes through this one fluent traversal source, never
 * through raw Gremlin query strings, so the exact same code path runs against either:
 *
 * <ul>
 *   <li>{@code plm.graph.mode=remote} (default) - a real Gremlin Server, e.g. the TinkerGraph-backed
 *       server in docker-compose for local dev, or Amazon Neptune in production.</li>
 *   <li>{@code plm.graph.mode=embedded} - an in-JVM TinkerGraph, useful for fast tests or a
 *       zero-dependency quick start.</li>
 * </ul>
 */
@org.springframework.context.annotation.Configuration
public class GremlinConfig {

    @Value("${plm.graph.mode:remote}")
    private String mode;

    @Value("${plm.graph.host:localhost}")
    private String host;

    @Value("${plm.graph.port:8182}")
    private int port;

    @Bean(destroyMethod = "close")
    public GraphTraversalSource graphTraversalSource() {
        if ("embedded".equalsIgnoreCase(mode)) {
            return embeddedTraversalSource();
        }
        Cluster cluster = Cluster.build()
                .addContactPoint(host)
                .port(port)
                .create();
        return AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(cluster, "g"));
    }

    /** An in-JVM TinkerGraph configured to accept our String vertex ids (part/supplier ids). */
    public static GraphTraversalSource embeddedTraversalSource() {
        org.apache.commons.configuration2.Configuration conf = new org.apache.commons.configuration2.BaseConfiguration();
        conf.setProperty("gremlin.tinkergraph.vertexIdManager", "ANY");
        conf.setProperty("gremlin.tinkergraph.edgeIdManager", "ANY");
        TinkerGraph graph = TinkerGraph.open(conf);
        return graph.traversal();
    }
}
