package com.mysupply.phase4.services;

import com.mysupply.phase4.models.ApiKey;
import com.mysupply.phase4.persistence.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    @Autowired
    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Validates if the provided API key is valid
     * @param apiKey The API key to validate
     * @return true if the API key is valid, false otherwise
     */
    @Transactional
    public boolean isValidApiKey(String apiKey) {

        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }

        Optional<ApiKey> storedKey = apiKeyRepository.findByKeyValue(apiKey);

        if (storedKey.isPresent()) {
            // Update last used timestamp
            ApiKey keyEntity = storedKey.get();
            keyEntity.setLastUsed(LocalDateTime.now());
            apiKeyRepository.save(keyEntity);
            return true;
        }

        return false;
    }

    /**
     * Gets the API key value for the specified key name
     * @param keyName The name of the API key
     * @return The API key value or null if not found
     */
    public String getApiKeyValue(String keyName) {
        Optional<ApiKey> apiKey = apiKeyRepository.findByKeyName(keyName);
        return apiKey.map(ApiKey::getKeyValue).orElse(null);
    }

    /**
     * Rotates (changes) the API key with a new randomly generated one
     * @param keyName The name of the key to rotate
     * @return The new API key value
     */
    @Transactional
    public String rotateApiKey(String keyName) {
        Optional<ApiKey> existingKey = apiKeyRepository.findByKeyName(keyName);

        if (existingKey.isEmpty()) {
            throw new RuntimeException("API key not found: " + keyName);
        }

        ApiKey keyEntity = existingKey.get();
        String newKeyValue = generateSecureRandomKey();
        keyEntity.setKeyValue(newKeyValue);
        keyEntity.setLastUsed(LocalDateTime.now());

        apiKeyRepository.save(keyEntity);
        return newKeyValue;
    }

    /**
     * Generates a cryptographically secure random API key
     * @return A new secure API key
     */
    private String generateSecureRandomKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64]; // 512 bits
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}