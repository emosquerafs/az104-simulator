package co.singularit.az104simulator.repository;

import co.singularit.az104simulator.domain.Attempt;
import co.singularit.az104simulator.domain.ExamMode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, String> {

    List<Attempt> findByIsCompletedOrderByStartedAtDesc(Boolean isCompleted);

    List<Attempt> findAllByOrderByStartedAtDesc();

    List<Attempt> findByStudentIdAndIsCompletedTrue(String studentId, Pageable pageable);

    List<Attempt> findByStudentIdAndModeAndIsCompletedTrue(String studentId, ExamMode mode, Pageable pageable);
}
