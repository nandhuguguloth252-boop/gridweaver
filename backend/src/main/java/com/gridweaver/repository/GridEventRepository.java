package com.gridweaver.repository;

import com.gridweaver.domain.GridEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GridEventRepository extends JpaRepository<GridEventEntity, Long> {
    List<GridEventEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
