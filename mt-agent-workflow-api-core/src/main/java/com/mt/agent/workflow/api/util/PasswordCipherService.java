package com.mt.agent.workflow.api.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordCipherService {
    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    public static byte[] encrypt(byte[] key, String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            SecretKeySpec keySpec = new SecretKeySpec(key, AES);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] cipherBytes = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);
            return buffer.array();
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    public static String decryptToString(byte[] key, byte[] cipherWithIv) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(cipherWithIv);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            SecretKeySpec keySpec = new SecretKeySpec(key, AES);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 加密字符串并返回Base64编码的字符串
     */
    public static String encryptToString(byte[] key, String plain) {
        byte[] encrypted = encrypt(key, plain);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 从Base64编码的字符串解密
     */
    public static String decryptToStringFromString(byte[] key, String base64Cipher) {
        byte[] cipherWithIv = Base64.getDecoder().decode(base64Cipher);
        return decryptToString(key, cipherWithIv);
    }
}


