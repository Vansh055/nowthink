package com.nowthink.repository;

import com.nowthink.model.Discovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DiscoveryRepository extends JpaRepository<Discovery, Long> {
    List<Discovery> findAllByOrderByCreatedAtDesc();
}
