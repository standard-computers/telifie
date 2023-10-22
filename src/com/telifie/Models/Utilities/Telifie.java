package com.telifie.Models.Utilities;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static com.telifie.Models.Andromeda.ALPHAS;
import static com.telifie.Models.Andromeda.NUMERALS;

public class Telifie {

    public static final int PRIVATE = 0, PROTECTED = 1, PUBLIC = 2;
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

    public static int epochTime(){
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static String randomReferenceCode(){
        return ALPHAS[(int) random(0, 25)] + ALPHAS[(int) random(0, 25)] + ALPHAS[(int) random(0, 25)] + ALPHAS[(int) random(0, 25)] + ALPHAS[(int) random(0, 25)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)] + NUMERALS[(int) random(0, 9)];
    }

    public static String shortEid(){
        return ALPHAS[(int) random(0, 25)] + NUMERALS[(int) random(0, 9)] + ALPHAS[(int) random(0, 25)] + NUMERALS[(int) random(0, 9)] + ALPHAS[(int) random(0, 25)] + NUMERALS[(int) random(0, 9)];
    }

    public static String simpleCode(){
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