package si.um.feri.__Backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import si.um.feri.__Backend.model.ApiKeySettings;
import si.um.feri.__Backend.repository.ApiKeyRepository;
import si.um.feri.__Backend.util.EncryptionUtils;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    @Value("${openai.api.key}")
    private String defaultKey;

    @Value("${encryption.secret}")
    private String encryptionSecret;

    public String getCurrentApiKey() {
        return apiKeyRepository.findById("singleton").map( setting -> {
                    try {
                        return EncryptionUtils.decrypt(setting.getEncryptedApiKey(), encryptionSecret);
                    } catch (Exception e) {
                        throw new RuntimeException("Decryption failed", e);
                    }
                })
                .orElse(defaultKey);
    }

    public void updateApiKey(String plainApiKey){
        try {
            String encrypted =  EncryptionUtils.encrypt(plainApiKey, encryptionSecret);
            ApiKeySettings setting = new ApiKeySettings();
            setting.setEncryptedApiKey(encrypted);
            apiKeyRepository.save(setting);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed",e);
        }
    }
}
