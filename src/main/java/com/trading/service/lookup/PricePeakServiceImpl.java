package com.trading.service.lookup;

import com.trading.domain.entity.PricePeak;
import com.trading.domain.repository.PricePeakRepository;
import com.trading.dto.lookup.PricePeakResponse;
import com.trading.dto.lookup.UpdatePricePeakRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PricePeakServiceImpl implements PricePeakService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PricePeakRepository pricePeakRepository;

    public PricePeakServiceImpl(PricePeakRepository pricePeakRepository) {
        this.pricePeakRepository = pricePeakRepository;
    }

    @Override
    public List<PricePeakResponse> list(UUID userId, String search) {
        Objects.requireNonNull(userId, "userId is required");
        String searchPattern = toSearchPattern(search);
        List<PricePeak> rows = searchPattern == null
            ? pricePeakRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId)
            : pricePeakRepository.findByUser_IdAndSearch(userId, searchPattern);
        return rows.stream()
            .map(PricePeakServiceImpl::toResponse)
            .toList();
    }

    @Override
    public PricePeakResponse update(UUID userId, UUID pricePeakId, UpdatePricePeakRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(pricePeakId, "pricePeakId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.peakPrice(), "peakPrice is required");
        Objects.requireNonNull(request.peakTimestamp(), "peakTimestamp is required");
        Objects.requireNonNull(request.active(), "active is required");
        if (request.peakPrice().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("peakPrice must be positive");
        }

        PricePeak peak = requirePricePeak(userId, pricePeakId);
        peak.setPeakPrice(request.peakPrice());
        peak.setPeakTimestamp(request.peakTimestamp());
        peak.setActive(request.active());
        peak.setUpdatedAt(OffsetDateTime.now());

        return toResponse(pricePeakRepository.save(peak));
    }

    @Override
    public void delete(UUID userId, UUID pricePeakId) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(pricePeakId, "pricePeakId is required");
        PricePeak peak = requirePricePeak(userId, pricePeakId);
        pricePeakRepository.delete(peak);
    }

    private PricePeak requirePricePeak(UUID userId, UUID pricePeakId) {
        return pricePeakRepository.findByIdAndUser_Id(pricePeakId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Price peak not found: " + pricePeakId));
    }

    private static String toSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim() + "%";
    }

    private static PricePeakResponse toResponse(PricePeak peak) {
        return new PricePeakResponse(
            peak.getId(),
            peak.getUser().getId(),
            peak.getAsset().getId(),
            peak.getAsset().getSymbol(),
            peak.getAsset().getName(),
            peak.getLastBuyTransaction() == null ? null : peak.getLastBuyTransaction().getId(),
            peak.getPeakPrice(),
            peak.getPeakTimestamp(),
            peak.getActive(),
            peak.getCreatedAt(),
            peak.getUpdatedAt()
        );
    }
}
