package com.lexpilot.graph.repository;

import com.lexpilot.graph.entity.GraphHyperedgeEntity;
import com.lexpilot.graph.entity.GraphHyperedgeMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GraphHyperedgeJpaRepo extends JpaRepository<GraphHyperedgeEntity, UUID> {

    /**
     * Find all hyperedges belonging to a repository.
     */
    List<GraphHyperedgeEntity> findByRepositoryId(UUID repositoryId);

    /**
     * Find member entries for a specific hyperedge.
     */
    @Query("SELECT m FROM GraphHyperedgeMemberEntity m WHERE m.hyperedgeId = :hyperedgeId")
    List<GraphHyperedgeMemberEntity> findMembersByHyperedgeId(@Param("hyperedgeId") UUID hyperedgeId);

    /**
     * Find all hyperedges that a given node belongs to.
     */
    @Query("""
        SELECT h FROM GraphHyperedgeEntity h
        WHERE h.id IN (
            SELECT m.hyperedgeId FROM GraphHyperedgeMemberEntity m
            WHERE m.nodeId = :nodeId
        )
        """)
    List<GraphHyperedgeEntity> findHyperedgesByNodeId(@Param("nodeId") UUID nodeId);
}
