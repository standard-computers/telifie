package com.telifie.Models.Utilities;

import com.telifie.Models.Actions.Out;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tool {

    private static final String[] ALPHAS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private static final String[] NUMERALS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    public static String eid(){
        return ALPHAS[(int) random(0, 25)] +
                ALPHAS[(int) random(0, 25)] +
                ALPHAS[(int) random(0, 25)] +
                ALPHAS[(int) random(0, 25)] +
                ALPHAS[(int) random(0, 25)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)];
    }

    public static String shortEid(){
        return ALPHAS[(int) random(0, 25)] +
                ALPHAS[(int) random(0, 25)] +
                ALPHAS[(int) random(0, 25)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)];
    }

    public static String simpleCode(){
        return NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)] +
                NUMERALS[(int) random(0, 9)];
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

    public static  String getType(String uri){
        if(uri.endsWith("html")){
            return "webpage";
        }else if(uri.endsWith("png") || uri.endsWith("svg") || uri.endsWith("gif") || uri.endsWith("jpeg") || uri.endsWith("jpg") || uri.endsWith("psd")){
            return "image";
        }else if(uri.endsWith("mp4") || uri.endsWith("wmv") || uri.endsWith("mov") || uri.endsWith("avi") || uri.endsWith("flv") || uri.endsWith("mkv")){
            return "video";
        }else if(uri.endsWith("wav") || uri.endsWith("mp3")){
            return "audio";
        }else if(uri.endsWith("pdf") || uri.endsWith("docx") || uri.endsWith("txt") || uri.endsWith("rtf")){
            return "document";
        }else if(uri.endsWith("php") || uri.endsWith("css")){
            return "code";
        }else{
            return "Unknown";
        }
    }

    public static Matcher findPhoneNumbers(String text){
        String regex = "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(text);
    }

    public static Matcher findEmails(String text){
        String regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}(\\\\s*,\\\\s*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,})";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(text);
    }

    public static Matcher findAddresses(String text){
        String regex = "\\\\d+\\\\s+([A-Za-z]+\\\\s+)+[A-Za-z]+,\\\\s+[A-Z]{2},\\\\s+\\\\d{5}";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(text);
    }

    public static String fixLink(String url, String src){
        if(src.startsWith("//")){
            src = "https:" + src;
        }else if(src.startsWith("/")){
            if(url.endsWith("/")){
                src = url.substring(0, url.length() - 1) + src;
            }else{
                src = url + src;
            }
        }
        return src;
    }

    public static String stripLink(String url){
        url = url.replace("https://", "");
        url = url.replace("www.", "");
        return url;

    }

    public static String concatArray(String[] array, int start, int end){
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public static int epochTime(){
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static boolean isHexColor(String value) {
        return value.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }

    public static boolean isHSLColor(String value) {
        return value.matches("^hsl\\(\\s*\\d+(\\.\\d+)?\\s*,\\s*\\d+(\\.\\d+)?%\\s*,\\s*\\d+(\\.\\d+)?%\\s*\\)$");
    }

    public static boolean isRGBColor(String value) {
        return value.matches("^rgb\\(\\s*\\d+(\\s*,\\s*\\d+){2}\\s*\\)$");
    }

    public static boolean containsAnyOf(String[] things, String string){
        for (String thing: things) {

            if(string.contains(thing)){
                return true;
            }
        }

        return false;
    }


    public static String formatNumber(double number){

        String numberStr = String.valueOf(number);
        if (number % 1 == 0) { // check if number is a whole number
            if (numberStr.indexOf('.') != -1 && numberStr.endsWith(".0")) {
                numberStr = numberStr.substring(0, numberStr.length() - 2);
            }
        }
        return numberStr;
    }

    public static String getWorkingDirectory(){
        String operatingSystem = System.getProperty("os.name");
        if(operatingSystem.equals("Mac OS X")){

            return Out.MAC_SYSTEM_DIR + "/connectors/";
        }else if(operatingSystem.startsWith("Windows")){

            return Out.WINDOWS_SYSTEM_DIR + "/connectors/";
        }else{

            return Out.UNIX_SYSTEM_DIR + "/connectors/";
        }
    }
}
