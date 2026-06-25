package com.trading.domain.repository;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Transaction> findOpenTransactions(
        UUID userId,
        String searchPattern,
        OffsetDateTime dateFromInclusive,
        OffsetDateTime dateToExclusive,
        Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Transaction> query = cb.createQuery(Transaction.class);
        Root<Transaction> root = query.from(Transaction.class);
        Join<Transaction, Asset> assetJoin = root.join("asset");
        Join<Transaction, Exchange> exchangeJoin = root.join("exchange");

        query.select(root).where(buildPredicates(
            cb,
            root,
            assetJoin,
            exchangeJoin,
            userId,
            searchPattern,
            dateFromInclusive,
            dateToExclusive
        ));
        query.orderBy(buildOrder(cb, root, assetJoin, exchangeJoin, pageable.getSort()));

        TypedQuery<Transaction> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<Transaction> content = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Transaction> countRoot = countQuery.from(Transaction.class);
        Join<Transaction, Asset> countAssetJoin = countRoot.join("asset");
        Join<Transaction, Exchange> countExchangeJoin = countRoot.join("exchange");
        countQuery.select(cb.count(countRoot)).where(buildPredicates(
            cb,
            countRoot,
            countAssetJoin,
            countExchangeJoin,
            userId,
            searchPattern,
            dateFromInclusive,
            dateToExclusive
        ));

        long total = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private static Predicate[] buildPredicates(
        CriteriaBuilder cb,
        Root<Transaction> root,
        Join<Transaction, Asset> assetJoin,
        Join<Transaction, Exchange> exchangeJoin,
        UUID userId,
        String searchPattern,
        OffsetDateTime dateFromInclusive,
        OffsetDateTime dateToExclusive
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("user").get("id"), userId));

        if (searchPattern != null) {
            predicates.add(cb.or(
                cb.like(cb.lower(assetJoin.get("symbol")), searchPattern),
                cb.like(cb.lower(assetJoin.get("name")), searchPattern),
                cb.like(cb.lower(exchangeJoin.get("symbol")), searchPattern),
                cb.like(cb.lower(exchangeJoin.get("name")), searchPattern)
            ));
        }
        if (dateFromInclusive != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), dateFromInclusive));
        }
        if (dateToExclusive != null) {
            predicates.add(cb.lessThan(root.get("transactionDate"), dateToExclusive));
        }

        return predicates.toArray(Predicate[]::new);
    }

    private static List<Order> buildOrder(
        CriteriaBuilder cb,
        Root<Transaction> root,
        Join<Transaction, Asset> assetJoin,
        Join<Transaction, Exchange> exchangeJoin,
        Sort sort
    ) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order sortOrder : sort) {
            jakarta.persistence.criteria.Path<?> path = switch (sortOrder.getProperty()) {
                case "asset.symbol" -> assetJoin.get("symbol");
                case "exchange.name" -> exchangeJoin.get("name");
                default -> root.get(sortOrder.getProperty());
            };
            orders.add(sortOrder.isAscending() ? cb.asc(path) : cb.desc(path));
        }
        return orders;
    }
}
