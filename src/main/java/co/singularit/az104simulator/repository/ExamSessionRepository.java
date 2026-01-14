package co.singularit.az104simulator.repository;

import co.singularit.az104simulator.domain.ExamMode;
import co.singularit.az104simulator.domain.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, String> {

    /**
     * Find all sessions by mode
     */
    List<ExamSession> findByMode(ExamMode mode);

    /**
     * Find active (not completed) sessions
     */
    List<ExamSession> findByCompletedAtIsNull();

    /**
     * Find completed sessions
     */
    List<ExamSession> findByCompletedAtIsNotNull();

    /**
     * Find sessions created after a specific date
     */
    List<ExamSession> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Check if a session exists and is not completed
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM ExamSession s WHERE s.id = :sessionId AND s.completedAt IS NULL")
    boolean existsActiveSession(@Param("sessionId") String sessionId);

    /**
     * Get session with questions eagerly loaded (for performance)
     */
    @Query("SELECT s FROM ExamSession s LEFT JOIN FETCH s.questions WHERE s.id = :sessionId")
    Optional<ExamSession> findByIdWithQuestions(@Param("sessionId") String sessionId);
}
