package com.nowthink.repository;

import com.nowthink.model.ThoughtEvolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThoughtEvolutionRepository extends JpaRepository<ThoughtEvolution, Long> {
    List<ThoughtEvolution> findByUserIdAndThemeOrderByRecordedAtAsc(String userId, String theme);
    List<ThoughtEvolution> findByUserIdOrderByRecordedAtDesc(String userId);
}