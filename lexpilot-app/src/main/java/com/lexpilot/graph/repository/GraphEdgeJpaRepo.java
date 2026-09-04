package com.lexpilot.graph.repository;

import com.lexpilot.graph.entity.GraphEdgeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GraphEdgeJpaRepo extends JpaRepository<GraphEdgeEntity, UUID> {

    /**
     * Find all edges belonging to a repository (paginated).
     */
    Page<GraphEdgeEntity> findByRepositoryId(UUID repositoryId, Pageable pageable);

    /**
     * Find all edges of a specific relation type within a repository.
     */
    Page<GraphEdgeEntity> findByRepositoryIdAndRelation(UUID repositoryId, String relation, Pageable pageable);

    /**
     * Find all edges where the given node is the source.
     */
    List<GraphEdgeEntity> findBySourceNodeId(UUID sourceNodeId);

    /**
     * Find all edges where the given node is the target.
     */
    List<GraphEdgeEntity> findByTargetNodeId(UUID targetNodeId);

    /**
     * Find all 1-hop neighbor edges for a given node (both directions).
     */
    @Query("""
        SELECT e FROM GraphEdgeEntity e
        WHERE e.sourceNodeId = :nodeId OR e.targetNodeId = :nodeId
        """)
    List<GraphEdgeEntity> findNeighborEdges(@Param("nodeId") UUID nodeId);

    /**
     * Count edges in a repository.
     */
    long countByRepositoryId(UUID repositoryId);
}
