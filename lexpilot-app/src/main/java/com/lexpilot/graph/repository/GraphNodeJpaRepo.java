package com.lexpilot.graph.repository;

import com.lexpilot.graph.entity.GraphNodeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GraphNodeJpaRepo extends JpaRepository<GraphNodeEntity, UUID> {

    /**
     * Find all nodes belonging to a repository (paginated).
     */
    Page<GraphNodeEntity> findByRepositoryId(UUID repositoryId, Pageable pageable);

    /**
     * Find all nodes in a specific community within a repository.
     */
    List<GraphNodeEntity> findByRepositoryIdAndCommunity(UUID repositoryId, int community);

    /**
     * Trigram fuzzy search on node labels within a repository.
     * Requires the pg_trgm extension and a GIN trigram index on norm_label.
     */
    @Query(value = """
        SELECT * FROM graph_nodes
        WHERE repository_id = :repoId
          AND norm_label % :searchTerm
        ORDER BY similarity(norm_label, :searchTerm) DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<GraphNodeEntity> searchByLabel(
            @Param("repoId") UUID repositoryId,
            @Param("searchTerm") String searchTerm,
            @Param("maxResults") int maxResults);

    /**
     * Find a node by its external identifier within a repository.
     */
    GraphNodeEntity findByRepositoryIdAndExternalId(UUID repositoryId, String externalId);

    /**
     * Count distinct communities in a repository.
     */
    @Query("SELECT DISTINCT n.community FROM GraphNodeEntity n WHERE n.repositoryId = :repoId AND n.community >= 0 ORDER BY n.community")
    List<Integer> findDistinctCommunitiesByRepositoryId(@Param("repoId") UUID repositoryId);

    /**
     * Count nodes in a repository.
     */
    long countByRepositoryId(UUID repositoryId);
}
