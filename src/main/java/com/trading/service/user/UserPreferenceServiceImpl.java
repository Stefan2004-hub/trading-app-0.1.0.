package com.trading.service.user;

import com.trading.domain.entity.User;
import com.trading.domain.entity.UserPreference;
import com.trading.domain.enums.BuyInputMode;
import com.trading.domain.repository.UserPreferenceRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.user.UserPreferenceResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;

    public UserPreferenceServiceImpl(UserPreferenceRepository userPreferenceRepository, UserRepository userRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserPreferenceResponse get(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        UserPreference preference = getOrCreate(userId);
        return toResponse(preference);
    }

    @Override
    public UserPreferenceResponse updateDefaultBuyInputMode(UUID userId, BuyInputMode mode) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(mode, "defaultBuyInputMode is required");

        UserPreference preference = getOrCreate(userId);
        preference.setDefaultBuyInputMode(mode);
        preference.setUpdatedAt(OffsetDateTime.now());

        UserPreference saved = userPreferenceRepository.save(preference);
        return toResponse(saved);
    }

    private UserPreference getOrCreate(UUID userId) {
        return userPreferenceRepository.findByUser_Id(userId)
            .orElseGet(() -> createDefaultPreference(userId));
    }

    private UserPreference createDefaultPreference(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserPreference preference = new UserPreference();
        preference.setUser(user);
        preference.setDefaultBuyInputMode(BuyInputMode.COIN_AMOUNT);
        preference.setCreatedAt(OffsetDateTime.now());
        preference.setUpdatedAt(OffsetDateTime.now());
        return userPreferenceRepository.save(preference);
    }

    private static UserPreferenceResponse toResponse(UserPreference preference) {
        return new UserPreferenceResponse(
            preference.getUser().getId(),
            preference.getDefaultBuyInputMode()
        );
    }
}
