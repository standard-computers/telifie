package com.telifie.Models.Utilities;

import com.google.common.html.HtmlEscapers;
import com.telifie.Models.Clients.Packages;
import com.twilio.rest.api.v2010.account.Message;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class Telifie {

    public static final String WINDOWS_SYSTEM_DIR = System.getenv("APPDATA") + "\\Telifie";
    public static final String MAC_SYSTEM_DIR = System.getProperty("user.home") + "/Library/Application Support/telifie";
    public static final String UNIX_SYSTEM_DIR = "/usr/bin/telifie/";
    public static final String[] ALPHAS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    public static final String[] NUMERALS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    public static String configDirectory(){
        String operatingSystem = System.getProperty("os.name");
        if(operatingSystem.equals("Mac OS X")){
            return Telifie.MAC_SYSTEM_DIR;
        }else if(operatingSystem.startsWith("Windows")){
            return Telifie.WINDOWS_SYSTEM_DIR;
        }else{
            return Telifie.UNIX_SYSTEM_DIR;
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
                            Log.console("FAILED TO DELETE FILE : " + file.getName());
                        }
                    }
                }
            } else {
                Log.console("TEMP DIR EMPTY");
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

    public static void sms(String to, String from, String content){
        String ACCOUNT_SID = Packages.get("com.telifie.connectors.twilio").getAccess();
        String AUTH_TOKEN = Packages.get("com.telifie.connectors.twilio").getSecret();
        com.twilio.Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message.creator(new com.twilio.type.PhoneNumber(to), new com.twilio.type.PhoneNumber(from), content).create();
    }

    public static boolean email(String email, String code){
        return false;
    }

    public static class tools {

        public static String escapeMarkdownForJson(String markdownText) {
            return markdownText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b").replace("\f", "\\f");
        }

        public static boolean contains(String[] things, String string){
            for (String thing: things) {
                if(string.contains(thing)){
                    return true;
                }
            }
            return false;
        }

        public static boolean startsWith(String[] things, String string){
            for (String thing: things) {
                if(string.startsWith(thing)){
                    return true;
                }
            }
            return false;
        }

        public static boolean equals(char sample, char[] chars){
            for(char ch : chars){
                if(sample == ch){
                    return true;
                }
            }
            return false;
        }

        public static String escape(String input){
            return new StringEscapeUtils().escapeJson(input);
        }

        public static String htmlEscape(String string){
            return  HtmlEscapers.htmlEscaper().escape(string);
        }

        public static String sentenceCase(String text) {
            if (text == null || text.isEmpty()) {
                return "";
            }
            Set<String> excludedWords = Set.of("of", "it", "to", "and");
            StringBuilder result = new StringBuilder();
            boolean capitalizeNext = true;
            for (String word : text.split("\\s+")) {
                String lowerCaseWord = word.toLowerCase();
                boolean isAbbreviation = lowerCaseWord.endsWith(".");
                if (capitalizeNext || !excludedWords.contains(lowerCaseWord) || isAbbreviation) {
                    result.append(isAbbreviation ? word : capitalize(word)).append(" ");
                    capitalizeNext = !isAbbreviation;
                } else {
                    result.append(word.toLowerCase());
                }
            }
            return result.toString().trim();
        }

        public static boolean containsAddress(String text){
            return text.matches("\\b\\d+\\s+([A-Za-z0-9.\\-'\\s]+)\\s+" +
                    "(St\\.?|Street|Rd\\.?|Road|Ave\\.?|Avenue|Blvd\\.?|Boulevard|Ln\\.?|Lane|Dr\\.?|Drive|Ct\\.?|Court)\\s+" +
                    "(\\w+),\\s+" +
                    "(\\w{2,})\\s+" +
                    "(\\d{5}(?:[-\\s]\\d{4})?)");
        }

        private static String capitalize(String word) {
            return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
        }

        public static int levenshtein(String x, String y) {
            int[][] dp = new int[x.length() + 1][y.length() + 1];
            for (int i = 0; i <= x.length(); i++) {
                for (int j = 0; j <= y.length(); j++) {
                    if (i == 0) {
                        dp[i][j] = j;
                    } else if (j == 0) {
                        dp[i][j] = i;
                    } else {
                        dp[i][j] = min(dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)), dp[i - 1][j] + 1, dp[i][j - 1] + 1);
                    }
                }
            }
            return dp[x.length()][y.length()];
        }

        private static int costOfSubstitution(char a, char b) {
            return a == b ? 0 : 1;
        }

        private static int min(int... numbers) {
            return java.util.Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
        }

        public static JSONObject geolocate(String address) throws IOException, InterruptedException {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.radar.io/v1/geocode/forward?query=" + URLEncoder.encode(address, StandardCharsets.UTF_8.toString()))).header("Authorization", Packages.get("com.telifie.connectors.radar").getAccess()).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray addressed = new JSONObject(response.body()).getJSONArray("addresses");
            if(addressed.length() > 0){
                return addressed.getJSONObject(0);
            }
            return null;
        }
    }

    public class crypter {

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
}