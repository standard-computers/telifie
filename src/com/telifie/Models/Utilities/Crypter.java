package com.telifie.Models.Utilities;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;

public class Crypter {

    public static void encrypt(String key, String fileIn, String fileOut) throws Exception {
        Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        try (FileInputStream fis = new FileInputStream(fileIn);
             FileOutputStream fos = new FileOutputStream(fileOut);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

            byte[] block = new byte[8];
            int i;
            while ((i = fis.read(block)) != -1) {
                cos.write(block, 0, i);
            }
        }
    }

    public static void decrypt(String key, String fileIn, String fileOut) throws Exception {
        Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
        var cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        try (FileInputStream fis = new FileInputStream(fileIn);
             CipherInputStream cis = new CipherInputStream(fis, cipher);
             FileOutputStream fos = new FileOutputStream(fileOut)) {

            byte[] block = new byte[8];
            int i;
            while ((i = cis.read(block)) != -1) {
                fos.write(block, 0, i);
            }
        }
    }

}
