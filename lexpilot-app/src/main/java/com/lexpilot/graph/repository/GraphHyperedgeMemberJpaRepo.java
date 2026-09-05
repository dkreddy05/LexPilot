package com.lexpilot.graph.repository;

import com.lexpilot.graph.entity.GraphHyperedgeMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link GraphHyperedgeMemberEntity}.
 * <p>
 * Created to fix the silent data loss in {@code GraphDataSeeder} where
 * member entities were constructed but never persisted (REC-2).
 */
@Repository
public interface GraphHyperedgeMemberJpaRepo
        extends JpaRepository<GraphHyperedgeMemberEntity, GraphHyperedgeMemberEntity.HyperedgeMemberId> {

    /**
     * Count members belonging to a specific hyperedge.
     */
    long countByHyperedgeId(UUID hyperedgeId);

    /**
     * Find all members belonging to a specific hyperedge.
     */
    List<GraphHyperedgeMemberEntity> findByHyperedgeId(UUID hyperedgeId);
}
