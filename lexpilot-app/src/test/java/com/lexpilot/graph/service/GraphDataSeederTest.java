package com.lexpilot.graph.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexpilot.graph.entity.GraphHyperedgeEntity;
import com.lexpilot.graph.entity.GraphHyperedgeMemberEntity;
import com.lexpilot.graph.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the GraphDataSeeder hyperedge member persistence fix (REC-2).
 * <p>
 * Seeds a fixture graph.json containing a hyperedge with 2 members,
 * then asserts that rows actually appear in graph_hyperedge_members.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GraphService.class)
class GraphDataSeederTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("lexpilot_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        // Disable auto-seed so we control when it runs
        registry.add("lexpilot.graph.auto-seed", () -> "false");
    }

    @Autowired
    private GraphService graphService;

    @Autowired
    private GraphRepositoryJpaRepo repositoryRepo;

    @Autowired
    private GraphHyperedgeJpaRepo hyperedgeRepo;

    @Autowired
    private GraphHyperedgeMemberJpaRepo hyperedgeMemberRepo;

    @Autowired
    private GraphNodeJpaRepo nodeRepo;

    @Autowired
    private GraphEdgeJpaRepo edgeRepo;

    private GraphDataSeeder seeder;

    @BeforeEach
    void setUp() {
        hyperedgeMemberRepo.deleteAll();
        hyperedgeRepo.deleteAll();
        edgeRepo.deleteAll();
        nodeRepo.deleteAll();
        repositoryRepo.deleteAll();

        seeder = new GraphDataSeeder(
                graphService, repositoryRepo, hyperedgeRepo,
                hyperedgeMemberRepo, new ObjectMapper());
    }

    @Test
    void seedFromGraphJson_shouldPersistHyperedgeMembers() throws IOException {
        // Create fixture graph.json with 3 nodes, 1 edge, and 1 hyperedge with 2 members
        String graphJson = """
                {
                  "nodes": [
                    {"id": "node-a", "label": "ModuleA", "file_type": "java", "community": 0},
                    {"id": "node-b", "label": "ModuleB", "file_type": "java", "community": 0},
                    {"id": "node-c", "label": "ModuleC", "file_type": "java", "community": 1}
                  ],
                  "links": [
                    {"source": "node-a", "target": "node-b", "relation": "calls"}
                  ],
                  "hyperedges": [
                    {
                      "label": "Test Hyperedge",
                      "description": "Groups A and B",
                      "edge_type": "architectural_group",
                      "members": [
                        {"node_id": "node-a", "role": "entry"},
                        {"node_id": "node-b", "role": "exit"}
                      ]
                    }
                  ]
                }
                """;

        Path tempFile = Files.createTempFile("test-graph", ".json");
        Files.writeString(tempFile, graphJson);

        try {
            seeder.seedFromGraphJson(tempFile);

            // Verify hyperedge was created
            List<GraphHyperedgeEntity> hyperedges = hyperedgeRepo.findAll();
            assertThat(hyperedges).hasSize(1);

            UUID hyperedgeId = hyperedges.get(0).getId();

            // Verify members were actually persisted (the bug: this was 0 before the fix)
            long memberCount = hyperedgeMemberRepo.countByHyperedgeId(hyperedgeId);
            assertThat(memberCount).isEqualTo(2);

            // Verify member details
            List<GraphHyperedgeMemberEntity> members = hyperedgeMemberRepo.findByHyperedgeId(hyperedgeId);
            assertThat(members)
                    .extracting(GraphHyperedgeMemberEntity::getRole)
                    .containsExactlyInAnyOrder("entry", "exit");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void seedFromGraphJson_shouldHandleHyperedgeWithUnresolvedNodes() throws IOException {
        // Hyperedge references a node that doesn't exist in the nodes array
        String graphJson = """
                {
                  "nodes": [
                    {"id": "node-a", "label": "ModuleA", "file_type": "java", "community": 0}
                  ],
                  "links": [],
                  "hyperedges": [
                    {
                      "label": "Partial Hyperedge",
                      "members": [
                        {"node_id": "node-a", "role": "member"},
                        {"node_id": "node-missing", "role": "member"}
                      ]
                    }
                  ]
                }
                """;

        Path tempFile = Files.createTempFile("test-graph-partial", ".json");
        Files.writeString(tempFile, graphJson);

        try {
            seeder.seedFromGraphJson(tempFile);

            List<GraphHyperedgeEntity> hyperedges = hyperedgeRepo.findAll();
            assertThat(hyperedges).hasSize(1);

            // Only 1 member should be persisted (node-a); node-missing is skipped
            UUID hyperedgeId = hyperedges.get(0).getId();
            long memberCount = hyperedgeMemberRepo.countByHyperedgeId(hyperedgeId);
            assertThat(memberCount).isEqualTo(1);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
