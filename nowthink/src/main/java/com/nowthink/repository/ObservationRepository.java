package com.nowthink.repository;

import com.nowthink.model.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, Long> {
    List<Observation> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Observation> findByUserIdOrderByCreatedAtAsc(String userId);
    long countByUserId(String userId);
}