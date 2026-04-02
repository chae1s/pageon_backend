package com.pageon.backend.common.utils;


import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class AesEncryptionUtil {

    @Value("${encryption.secret-key}")
    private String secretKey;

    public String encrypt(String plainText) {

        try {

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            byte[] decodedKey = Base64.getDecoder().decode(secretKey);

            SecretKeySpec secretKeySpec = new SecretKeySpec(decodedKey, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] encryptedData = cipher.doFinal(plainText.getBytes());

            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String encryptedDataBase64 = Base64.getEncoder().encodeToString(encryptedData);

            return ivBase64 + ":" + encryptedDataBase64;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CustomException(ErrorCode.ENCRYPTION_FAILED);
        }

    }

    public String decrypt(String encryptedText) {
        try {
            String[] parts = encryptedText.split(":");
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            byte[] decodedKey = Base64.getDecoder().decode(secretKey);

            SecretKeySpec secretKeySpec = new SecretKeySpec(decodedKey, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encrypted);

            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CustomException(ErrorCode.DECRYPTION_FAILED);
        }
    }
}
