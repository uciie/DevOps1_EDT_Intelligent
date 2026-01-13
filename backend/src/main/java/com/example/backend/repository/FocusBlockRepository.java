package com.example.backend.repository;

import com.example.backend.model.FocusBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FocusBlockRepository extends JpaRepository<FocusBlock, Long> {
    List<FocusBlock> findByUser_IdAndStartTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
}

