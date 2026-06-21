package org.reciclame.demo.repository;

import org.reciclame.demo.model.ChatbotFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ChatbotFeedbackRepository extends JpaRepository<ChatbotFeedback, Long> {

    @Query("""
            SELECT COUNT(f) FROM ChatbotFeedback f
            WHERE f.calificacion = 1
              AND (:desde IS NULL OR f.createdAt >= :desde)
              AND (:hasta IS NULL OR f.createdAt <= :hasta)
            """)
    long countPositivo(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(f) FROM ChatbotFeedback f
            WHERE f.calificacion = -1
              AND (:desde IS NULL OR f.createdAt >= :desde)
              AND (:hasta IS NULL OR f.createdAt <= :hasta)
            """)
    long countNegativo(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}