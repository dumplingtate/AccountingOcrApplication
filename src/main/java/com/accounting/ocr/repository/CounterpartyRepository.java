package com.accounting.ocr.repository;

import com.accounting.ocr.model.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {

    Optional<Counterparty> findByInn(String inn);

    boolean existsByInn(String inn);

}