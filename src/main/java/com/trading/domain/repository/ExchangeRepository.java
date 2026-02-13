package com.trading.domain.repository;

import com.trading.domain.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRepository extends JpaRepository<Exchange, UUID> {

    Optional<Exchange> findByNameIgnoreCase(String name);

    List<Exchange> findAllByOrderByNameAsc();
}
