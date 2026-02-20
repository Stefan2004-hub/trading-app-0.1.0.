package com.trading.service.lookup;

import com.trading.domain.entity.Exchange;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.dto.lookup.ExchangeLookupResponse;
import com.trading.dto.lookup.UpsertExchangeRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class ExchangeServiceImpl implements ExchangeService {

    private final ExchangeRepository exchangeRepository;

    public ExchangeServiceImpl(ExchangeRepository exchangeRepository) {
        this.exchangeRepository = exchangeRepository;
    }

    @Override
    public List<ExchangeLookupResponse> list(String search) {
        String normalized = normalizeSearch(search);
        List<Exchange> rows = normalized == null
            ? exchangeRepository.findAllByOrderByNameAsc()
            : exchangeRepository.findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(
                normalized,
                normalized
            );
        return rows.stream()
            .map(ExchangeServiceImpl::toResponse)
            .toList();
    }

    @Override
    public ExchangeLookupResponse get(UUID id) {
        Objects.requireNonNull(id, "id is required");
        return toResponse(requireExchange(id));
    }

    @Override
    public ExchangeLookupResponse create(UpsertExchangeRequest request) {
        Objects.requireNonNull(request, "request is required");
        String symbol = normalizeSymbol(request.symbol());
        String name = normalizeName(request.name());

        assertUniqueName(name, null);
        assertUniqueSymbol(symbol, null);

        Exchange exchange = new Exchange();
        exchange.setSymbol(symbol);
        exchange.setName(name);
        return toResponse(exchangeRepository.save(exchange));
    }

    @Override
    public ExchangeLookupResponse update(UUID id, UpsertExchangeRequest request) {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(request, "request is required");
        String symbol = normalizeSymbol(request.symbol());
        String name = normalizeName(request.name());

        Exchange exchange = requireExchange(id);
        assertUniqueName(name, id);
        assertUniqueSymbol(symbol, id);

        exchange.setSymbol(symbol);
        exchange.setName(name);
        return toResponse(exchangeRepository.save(exchange));
    }

    @Override
    public void delete(UUID id) {
        Objects.requireNonNull(id, "id is required");
        Exchange exchange = requireExchange(id);
        exchangeRepository.delete(exchange);
    }

    private Exchange requireExchange(UUID id) {
        return exchangeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Exchange not found: " + id));
    }

    private void assertUniqueName(String name, UUID currentId) {
        exchangeRepository.findByNameIgnoreCase(name)
            .filter((row) -> !row.getId().equals(currentId))
            .ifPresent((row) -> {
                throw new IllegalArgumentException("Exchange name already exists: " + name);
            });
    }

    private void assertUniqueSymbol(String symbol, UUID currentId) {
        exchangeRepository.findBySymbolIgnoreCase(symbol)
            .filter((row) -> !row.getId().equals(currentId))
            .ifPresent((row) -> {
                throw new IllegalArgumentException("Exchange symbol already exists: " + symbol);
            });
    }

    private static String normalizeSymbol(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private static ExchangeLookupResponse toResponse(Exchange exchange) {
        return new ExchangeLookupResponse(exchange.getId(), exchange.getSymbol(), exchange.getName());
    }
}
