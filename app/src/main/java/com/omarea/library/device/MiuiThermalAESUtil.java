package com.omarea.library.device;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MiuiThermalAESUtil {
    private static final String KEY = "thermalopenssl.h";
    private static final String OFFSET = "thermalopenssl.h";
    private static final String ENCODING = "UTF-8";
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    public static byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        SecretKeySpec skeySpec = new SecretKeySpec(KEY.getBytes(ENCODING), ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(OFFSET.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] buffer) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        SecretKeySpec skeySpec = new SecretKeySpec(KEY.getBytes(ENCODING), ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(OFFSET.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        return cipher.doFinal(buffer);
    }

    private static byte[] decryptFile(String path) throws Exception {
        FileInputStream input = new FileInputStream(path);
        byte[] buffer = new byte[input.available()];
        input.read(buffer);

        return decrypt(buffer);
    }

    private static void encryptFile(String path) throws Exception {
        FileInputStream input = new FileInputStream(path);
        byte[] buffer = new byte[input.available()];
        input.read(buffer);

        byte[] result = encrypt(buffer);

        String outputPath = path.replace("_decrypted.conf", "");
        System.out.println(outputPath);
        input.close();
        FileOutputStream outputStream = new FileOutputStream(outputPath);
        outputStream.write(result);
    }
}