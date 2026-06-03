package com.hti.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CryptoUtil {

    @Value("${crypto.secret-key}")
    private String secretKey;

    public String decrypt(String encryptedText) {
        try {

            SecretKeySpec key =
                    new SecretKeySpec(secretKey.getBytes(), "AES");

            Cipher cipher =
                    Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decrypted =
                    cipher.doFinal(Base64.getDecoder()
                            .decode(encryptedText));

            return new String(decrypted);

        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt");
        }
    }
    
    public String encrypt(String text) {
        try {
            System.out.println("Key length: " + secretKey.getBytes().length);
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            e.printStackTrace(); // ← actual error dekho
            throw new RuntimeException("Unable to encrypt", e);
        }
    }
    
}