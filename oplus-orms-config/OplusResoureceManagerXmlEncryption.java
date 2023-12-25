//
// Decompiled by Jadx - 559ms
//
package com.omarea.library.basic;

import android.annotation.SuppressLint;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Example
 * OplusResoureceManagerXmlEncryption.decryptXmlFile("/odm/etc/orms/orms_core_config.xml")
 */

@SuppressLint("NewApi")
public class OplusResoureceManagerXmlEncryption {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static Charset charset = StandardCharsets.UTF_8;
    private static final int ivLengthByte = 12;
    private static final int keyLength = 16;
    private static final int tagLengthBit = 128;

    public static String readFile(String path) {
        String content = null;
        try {
            FileInputStream fis = new FileInputStream(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                BufferedInputStream bis = new BufferedInputStream(fis);
                for (int result = bis.read(); result != -1; result = bis.read()) {
                    bos.write((byte) result);
                }
                content = bos.toString("UTF-8");
                bis.close();
                bos.close();
                fis.close();
            } catch (Throwable th) {
                try {
                    bos.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    public static void writeFile(String input, String outputPath) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(outputPath));
            BufferedOutputStream buf = new BufferedOutputStream(fos);
            buf.write(input.getBytes());
            buf.flush();
            buf.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String encrypt(byte[] key, String rawData) throws Exception {
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[ivLengthByte];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(1, new SecretKeySpec(key, "AES"), new GCMParameterSpec(tagLengthBit, iv));
            byte[] encrypted = cipher.doFinal(rawData.getBytes(charset));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + 1 + encrypted.length);
            byteBuffer.put((byte) iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public static String decrypt(byte[] key, String encryptedData) throws Exception {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getMimeDecoder().decode(encryptedData));
            int ivLength = byteBuffer.get();
            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(2, new SecretKeySpec(key, "AES"), new GCMParameterSpec(tagLengthBit, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            Arrays.fill(iv, (byte) 0);
            Arrays.fill(encrypted, (byte) 0);
            return new String(decrypted, charset);
        } catch (Exception e) {
            throw new Exception("could not decrypt", e);
        }
    }

    public static String getSha256Key() {
        String s = "ORMS"; // SystemProperties.get("persist.sys.orms.name", "");
        return getSHA256(s).substring(0, keyLength);
    }

    public static String getSHA256(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes("UTF-8"));
            String encodestr = byte2Hex(messageDigest.digest());
            return encodestr;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        } catch (NoSuchAlgorithmException e2) {
            e2.printStackTrace();
            return "";
        }
    }

    private static String byte2Hex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (byte b : bytes) {
            String temp = Integer.toHexString(b & 255);
            if (temp.length() == 1) {
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    private static String findinStr(String xmlstr, String tag) {
        String tagBegin = "<" + tag + ">";
        String tagEnd = "</" + tag + ">";
        int iBegin = xmlstr.indexOf(tagBegin) + tagBegin.length();
        int iEnd = xmlstr.indexOf(tagEnd);
        if (iBegin <= -1 || iEnd <= -1 || iEnd <= iBegin) {
            return "";
        }
        String value = xmlstr.substring(iBegin, iEnd);
        return value;
    }

    public static InputStream decryptXmlFile(String srcPath) throws Exception {
        try {
            String srcContent = readFile(srcPath);
            String encryptString = findinStr(srcContent, "content").trim();
            if (srcContent != null && encryptString != null) {
                if (encryptString.equals("")) {
                    String decryptString = decrypt(getSha256Key().getBytes(), srcContent);
                    InputStream inputstream = new ByteArrayInputStream(decryptString.getBytes());
                    return inputstream;
                }
                String decryptString2 = decrypt(getSha256Key().getBytes(), encryptString);
                InputStream inputstream2 = new ByteArrayInputStream(decryptString2.getBytes());
                return inputstream2;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
