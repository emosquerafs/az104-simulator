package co.singularit.az104simulator.repository;

import co.singularit.az104simulator.domain.ExamSessionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSessionQuestionRepository extends JpaRepository<ExamSessionQuestion, Long> {

    /**
     * Find a specific question by session ID and position
     */
    @Query("SELECT sq FROM ExamSessionQuestion sq " +
           "JOIN FETCH sq.question q " +
           "LEFT JOIN FETCH q.options " +
           "WHERE sq.session.id = :sessionId AND sq.position = :position")
    Optional<ExamSessionQuestion> findBySessionIdAndPosition(
        @Param("sessionId") String sessionId,
        @Param("position") Integer position
    );

    /**
     * Find all questions for a session ordered by position
     */
    @Query("SELECT sq FROM ExamSessionQuestion sq " +
           "WHERE sq.session.id = :sessionId " +
           "ORDER BY sq.position ASC")
    List<ExamSessionQuestion> findBySessionIdOrderByPosition(@Param("sessionId") String sessionId);

    /**
     * Find all questions for a session with question details eagerly loaded
     */
    @Query("SELECT sq FROM ExamSessionQuestion sq " +
           "JOIN FETCH sq.question q " +
           "LEFT JOIN FETCH q.options " +
           "WHERE sq.session.id = :sessionId " +
           "ORDER BY sq.position ASC")
    List<ExamSessionQuestion> findBySessionIdWithQuestionsOrderByPosition(@Param("sessionId") String sessionId);

    /**
     * Check if a question already exists in a session
     */
    boolean existsBySessionIdAndQuestionId(String sessionId, Long questionId);

    /**
     * Count questions in a session
     */
    long countBySessionId(String sessionId);

    /**
     * Get the highest position in a session
     */
    @Query("SELECT MAX(sq.position) FROM ExamSessionQuestion sq WHERE sq.session.id = :sessionId")
    Optional<Integer> findMaxPositionBySessionId(@Param("sessionId") String sessionId);

    /**
     * Get all question IDs for a session (for efficient checking)
     */
    @Query("SELECT sq.question.id FROM ExamSessionQuestion sq WHERE sq.session.id = :sessionId")
    List<Long> findQuestionIdsBySessionId(@Param("sessionId") String sessionId);
}
