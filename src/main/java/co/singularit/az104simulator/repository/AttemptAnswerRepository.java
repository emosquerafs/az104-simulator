package co.singularit.az104simulator.repository;

import co.singularit.az104simulator.domain.Attempt;
import co.singularit.az104simulator.domain.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByAttempt(Attempt attempt);

    List<AttemptAnswer> findByAttemptOrderByPositionAsc(Attempt attempt);

    Optional<AttemptAnswer> findByAttemptAndQuestionId(Attempt attempt, Long questionId);

    long countByAttemptAndSelectedOptionIdsJsonIsNotNull(Attempt attempt);

    long countByAttemptAndMarkedTrue(Attempt attempt);
}
