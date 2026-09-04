package com.lexpilot.graph.repository;

import com.lexpilot.graph.entity.GraphRepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GraphRepositoryJpaRepo extends JpaRepository<GraphRepositoryEntity, UUID> {
}
