package com.example.aigc.service;

import com.example.aigc.config.EncryptionProperties;
import com.example.aigc.exception.BizException;
import com.example.aigc.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ApiKeyCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final EncryptionProperties encryptionProperties;
    private byte[] secretKey;

    public ApiKeyCryptoService(EncryptionProperties encryptionProperties) {
        this.encryptionProperties = encryptionProperties;
    }

    @PostConstruct
    public void init() {
        try {
            String rawKey = encryptionProperties.getEncryptionKey();
            if (rawKey == null || rawKey.isBlank()) {
                throw new BizException(500, ErrorCode.INTERNAL_ERROR, "未配置加密密钥");
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.secretKey = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(500, ErrorCode.INTERNAL_ERROR, "加密服务初始化失败");
        }
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception ex) {
            throw new BizException(500, ErrorCode.INTERNAL_ERROR, "API Key 加密失败");
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BizException(500, ErrorCode.INTERNAL_ERROR, "API Key 解密失败");
        }
    }

    public String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (apiKey.length() <= 6) {
            return "*".repeat(apiKey.length());
        }
        String prefix = apiKey.substring(0, 3);
        String suffix = apiKey.substring(apiKey.length() - 3);
        return prefix + "*".repeat(apiKey.length() - 6) + suffix;
    }
}