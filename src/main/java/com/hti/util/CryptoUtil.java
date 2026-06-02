package com.hti.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

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
    
    
}