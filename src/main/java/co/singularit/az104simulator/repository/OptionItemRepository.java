package co.singularit.az104simulator.repository;

import co.singularit.az104simulator.domain.OptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionItemRepository extends JpaRepository<OptionItem, Long> {
}
