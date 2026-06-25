package com.trading.domain.repository;

import com.trading.domain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface TransactionRepositoryCustom {

    Page<Transaction> findOpenTransactions(
        UUID userId,
        String searchPattern,
        OffsetDateTime dateFromInclusive,
        OffsetDateTime dateToExclusive,
        Pageable pageable
    );
}
