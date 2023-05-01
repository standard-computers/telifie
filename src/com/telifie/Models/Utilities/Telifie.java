package com.telifie.Models.Utilities;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Telifie {

    public static String[] stopWords = new String[]{"a", "an", "and", "are", "as", "at", "make", "be", "by", "for", "from", "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "with", "who", "what", "when", "where", "why", "how"};
    private static final String[] ALPHAS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private static final String[] NUMERALS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    public static final int PRIVATE = 0, PROTECTED = 1, PUBLIC = 2;
    public static final String WINDOWS_SYSTEM_DIR = "/Program\\ Files/telifie/";
    public static final String MAC_SYSTEM_DIR = System.getProperty("user.home") + "/Library/Application Support/telifie";
    public static final String UNIX_SYSTEM_DIR = "/usr/bin/telifie/";

    public enum Languages {

        ENGLISH("ENGLISH"),
        SPANISH("SPANISH"),
        FRENCH("FRENCH"),
        CHINESE("CHINESE"),
        GERMAN("GERMAN");

        private String displayName = "ENGLISH";
        private Languages(String displayName){
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    public static String getConfigDirectory(){
        String operatingSystem = System.getProperty("os.name");
        if(operatingSystem.equals("Mac OS X")){
            return Telifie.MAC_SYSTEM_DIR + "/";
        }else if(operatingSystem.startsWith("Windows")){
            return Telifie.WINDOWS_SYSTEM_DIR + "/";
        }else{
            return Telifie.UNIX_SYSTEM_DIR + "/";
        }
    }

    public static String getConnectorsDirectory(){
        String operatingSystem = System.getProperty("os.name");
        if(operatingSystem.equals("Mac OS X")){
            return Telifie.MAC_SYSTEM_DIR + "/connectors/";
        }else if(operatingSystem.startsWith("Windows")){
            return Telifie.WINDOWS_SYSTEM_DIR + "/connectors/";
        }else{
            return Telifie.UNIX_SYSTEM_DIR + "/connectors/";
        }
    }

    public static int getEpochTime(){
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static class files {

        private static File file;

        public static File file(String directory, String name, String content){
            File dir = new File(directory);
            if(!dir.exists()){
                dir.mkdirs();
            }
            return (files.file = new File(name));
        }

        public static boolean write(String content){
            return false;
        }

        public static void serialized(String name, Serializable object){
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(name));
                out.writeObject(object);
                out.flush();
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class console {

        public static class out {

            public static void line(){
                System.out.println("--------------------------------------------------------------");
            }

            public static void message(String message){
                line();
                error("\n" + message + "\n");
                line();
            }

            public static void string(String message){
                System.out.println(message);
            }

            public static void error(String message){
                System.err.println(message);
            }

            public static void telifie() {
                System.out.println("\n");
                System.out.println("||===========================================================||");
                System.out.println("||                                                           ||");
                System.out.println("||  ,--------. ,------. ,--.    ,--. ,------. ,--. ,------.  ||");
                System.out.println("||  '--.  .--' |  .---' |  |    |  | |  .---' |  | |  .---'  ||");
                System.out.println("||     |  |    |  `--,  |  |    |  | |  `--,  |  | |  `--,   ||");
                System.out.println("||     |  |    |  `---. |  '--. |  | |  |`    |  | |  `---.  ||");
                System.out.println("||     `--'    `------' `-----' `--' `--'     `--' `------'  ||");
                System.out.println("||                                                           ||");
                System.out.println("||===========================================================||\n");
            }
        }

        public static class in {

            public static String string(){
                Scanner in = new Scanner(System.in);
                return in.nextLine();
            }

            public static String string(String prompt){
                System.out.print(prompt);
                Scanner in = new Scanner(System.in);
                return in.nextLine();
            }

            public static Object serialized(String dir){
                try {
                    ObjectInputStream in = new ObjectInputStream(new FileInputStream(dir));
                    Object obj = in.readObject();
                    return obj;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

//            public static String download(String get, String put){
//
//            }
        }
    }

    public static class tools {

        public static class strings {

            public static String sentenceCase(String str) {
                if (str == null || str.isEmpty()) {
                    return str;
                }
                String[] words = str.trim().split("\\s+");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    sb.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase())
                            .append(" ");
                }
                return sb.toString().trim();
            }

            public static boolean containsAnyOf(String[] things, String string){
                for (String thing: things) {
                    if(string.contains(thing)){
                        return true;
                    }
                }
                return false;
            }

            public static String removeWords(String text, String[] wordsToRemove) {
                StringBuilder sb = new StringBuilder();
                String[] words = text.split("\\s+");
                for (String word : words) {
                    if (!Arrays.asList(wordsToRemove).contains(word.toLowerCase())) {
                        sb.append(word).append(" ");
                    }
                }
                return sb.toString().trim();
            }

            public static boolean equals(char sample, char[] chars){
                for(char ch : chars){
                    if(sample == ch){
                        return true;
                    }
                }
                return false;
            }

            public static String escape(String string){
                return  StringEscapeUtils.escapeJson(string);
            }

            public static String htmlEscape(String string){
                return  StringEscapeUtils.escapeHtml4(string);
            }
        }

        public static class make {

            public static String randomReferenceCode(){
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
                        NUMERALS[(int) random(0, 9)] +
                        ALPHAS[(int) random(0, 25)] +
                        NUMERALS[(int) random(0, 9)] +
                        ALPHAS[(int) random(0, 25)] +
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

            public static String formatNumber(double number){
                String numberStr = String.valueOf(number);
                if (number % 1 == 0) { // check if number is a whole number
                    if (numberStr.indexOf('.') != -1 && numberStr.endsWith(".0")) {
                        numberStr = numberStr.substring(0, numberStr.length() - 2);
                    }
                }
                return numberStr;
            }

            public static ArrayList<String> extractLinks(Elements elements, String root){
                ArrayList<String> links = new ArrayList<>();
                for(Element el : elements){
                    String link = el.attr("href");
                    if(!link.equals("/") && !link.equals("") & !link.equals(root) && !Telifie.tools.strings.containsAnyOf(new String[]{"facebook", "instagram", "spotify", "linkedin", "youtube"}, link)){
                        String fixed = Telifie.tools.detector.fixLink(root, link);
                        if(Telifie.tools.detector.isValidLink(fixed, root)){
                            links.add(fixed);
                        }
                    }
                }
                return links;
            }
        }

        public static class detector {

            public static String fileExtension(String uri) {
                Path path = Paths.get(uri);
                String fileName = path.getFileName().toString();
                int dotIndex = fileName.lastIndexOf(".");
                if (dotIndex > 0) {
                    return fileName.substring(dotIndex + 1);
                } else {
                    return "";
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
                    src = "https:" + src.trim();
                }else if(src.startsWith("/")){
                    if(url.endsWith("/")){
                        src = url.substring(0, url.length() - 1) + src;
                    }else{
                        src = url + src;
                    }
                }
                return src;
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

            public static boolean isUrl(String uri){
                return uri.startsWith("https://") || uri.startsWith("http://") || uri.startsWith("www");
            }

            public static boolean isFile(String uri){
                return uri.startsWith("file://") || uri.startsWith("c:/") || uri.startsWith("\\");
            }

            public static String getType(String uri){
                if(isUrl(uri) || uri.endsWith("html")){
                    return "webpage";
                }else if(uri.endsWith("png") || uri.endsWith("gif") || uri.endsWith("jpeg") || uri.endsWith("jpg") || uri.endsWith("psd")){
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



            public static String fileToString(String filePath) {
                File file = new File(filePath);
                if(!file.exists()){
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return sb.toString();
            }

            public static boolean isValidLink(String link, String uri) {
                if(link.contains("cart") || link.contains("search") || link.contains("account")){ //Audit out pages
                    return false;
                }
                if (link.startsWith("tel:")
                        && link.startsWith("mailto:")
                        && link.startsWith("sms:")
                        && link.startsWith("skype:")
                        && link.startsWith("#")) {
                    return false;
                }
                return true;
            }
        }
    }
}
