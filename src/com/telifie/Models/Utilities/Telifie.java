package com.telifie.Models.Utilities;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static com.telifie.Models.Andromeda.Andromeda.ALPHAS;
import static com.telifie.Models.Andromeda.Andromeda.NUMERALS;

public class Telifie {

    public static final String WINDOWS_SYSTEM_DIR = "/Program\\ Files/telifie/";
    public static final String MAC_SYSTEM_DIR = System.getProperty("user.home") + "/Library/Application Support/telifie";
    public static final String UNIX_SYSTEM_DIR = "/usr/bin/telifie/";

    public static String configDirectory(){
        String operatingSystem = System.getProperty("os.name");
        if(operatingSystem.equals("Mac OS X")){
            return Telifie.MAC_SYSTEM_DIR + "/";
        }else if(operatingSystem.startsWith("Windows")){
            return Telifie.WINDOWS_SYSTEM_DIR + "/";
        }else{
            return Telifie.UNIX_SYSTEM_DIR + "/";
        }
    }

    public static void purgeTemp(){
        File tempDir = new File(Telifie.configDirectory() + "temp");
        if(tempDir.exists() && tempDir.isDirectory()){
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        if (!file.delete()) {
                            Console.log("FAILED TO DELETE FILE : " + file.getName());
                        }
                    }
                }
            } else {
                Console.log("TEMP DIR EMPTY");
            }
        }
    }

    public static int epochTime(){
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static String randomReferenceCode(){
        return ALPHAS[(int) random(0, 25)] + ALPHAS[(int) random(0, 25)] + ALPHAS[(int) random(0, 25)] + ALPHAS[(int) random(0, 25)] + ALPHAS[(int) random(0, 25)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)];
    }

    public static String digitCode(){
        return NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)];
    }

    public static double random(double low, double high){
        return ((Math.random() * high) - low);
    }

    public static String md5(String input){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}