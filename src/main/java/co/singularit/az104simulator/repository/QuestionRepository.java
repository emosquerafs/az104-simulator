package co.singularit.az104simulator.repository;

import co.singularit.az104simulator.domain.Difficulty;
import co.singularit.az104simulator.domain.Domain;
import co.singularit.az104simulator.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByDomain(Domain domain);

    List<Question> findByDifficulty(Difficulty difficulty);

    List<Question> findByDomainAndDifficulty(Domain domain, Difficulty difficulty);

    @Query("SELECT q FROM Question q WHERE q.domain IN :domains")
    List<Question> findByDomainIn(List<Domain> domains);

    @Query("SELECT q FROM Question q WHERE q.domain IN :domains AND q.difficulty = :difficulty")
    List<Question> findByDomainInAndDifficulty(List<Domain> domains, Difficulty difficulty);

    @Query("SELECT COUNT(q) FROM Question q")
    long countAll();

    @Query("SELECT COUNT(q) FROM Question q WHERE q.domain = :domain")
    long countByDomain(Domain domain);
}
