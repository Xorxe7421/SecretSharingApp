package org.pavl.secretsharingapp.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class CryptoUtils {

    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public static String generateAccessToken() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timeComponent = Long.toHexString(System.nanoTime());
        return uuid + timeComponent;
    }

    public static String generateApiKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String encryptContent(String content, String masterKey) {
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            byte[] randomIV = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(randomIV);

            byte[] masterKeyBytes = Base64.getDecoder().decode(masterKey);
            SecretKeySpec key = new SecretKeySpec(masterKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, randomIV);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] ciphertext = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(randomIV, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decryptContent(String encryptedContent, String masterKey) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedContent);

            byte[] ivBytes = Arrays.copyOfRange(encryptedBytes, 0, IV_LENGTH);
            byte[] cyberTextBytes = Arrays.copyOfRange(encryptedBytes, IV_LENGTH, encryptedBytes.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);
            byte[] masterKeyBytes = Base64.getDecoder().decode(masterKey);
            SecretKeySpec key = new SecretKeySpec(masterKeyBytes, "AES");
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(cyberTextBytes);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
