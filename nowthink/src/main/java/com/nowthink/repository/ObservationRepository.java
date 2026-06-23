package com.nowthink.repository;

import com.nowthink.model.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, Long> {
    List<Observation> findAllByOrderByCreatedAtDesc();
    List<Observation> findAllByOrderByCreatedAtAsc();
    long count();
}