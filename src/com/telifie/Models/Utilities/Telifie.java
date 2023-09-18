package com.telifie.Models.Utilities;

import com.mongodb.client.*;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import com.telifie.Models.Article;
import org.apache.commons.lang3.StringEscapeUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Telifie {

    public static String[] stopWords = new String[]{"a", "an", "and", "are", "as", "at", "or", "make", "be", "by", "for", "from", "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "with", "who", "what", "when", "where", "why", "how", "you"};
    private static final String[] ALPHAS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private static final String[] NUMERALS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    public static final String[] PROXIMITY = {"near", "nearby", "close to", "around", "within", "in the vicinity of", "within walking distance of", "adjacent to", "bordering", "neighboring", "local to", "surrounding", "not far from", "just off"};
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

    public static class tools {

        public static void geocode(Configuration config){
            MongoClient mongoClient = MongoClients.create(config.getURI());
            MongoDatabase database = mongoClient.getDatabase("telifie");
            MongoCollection<Document> collection = database.getCollection("articles");
            FindIterable<Document> doc = collection.find(new Document("$and", Arrays.asList(
                    new Document("attributes.key", "Longitude"),
                    new Document("attributes.key", "Latitude"),
                    new Document("location", new Document("$exists", false))
            )));
            for(Document d : doc){
                if (d != null) {
                    Article a = new Article(d);
                    String longitude = a.getAttribute("Longitude");
                    String latitude = a.getAttribute("Latitude");
                    if(longitude != null && latitude != null && !longitude.equals("null") && !latitude.equals("null")){
                        double longitudeValue = Double.parseDouble(longitude);
                        double latitudeValue = Double.parseDouble(latitude);
                        Position position = new Position(longitudeValue, latitudeValue);
                        Point point = new Point(position);
                        Bson update = Updates.set("location", point);
                        collection.updateOne(new Document("id", a.getId()), update);
                        System.out.println(a.getTitle());
                    }
                }
            }
            mongoClient.close();
        }

        public static String escapeMarkdownForJson(String markdownText) {
            String escapedText = markdownText.replace("\\", "\\\\");
            escapedText = escapedText.replace("\"", "\\\"");
            escapedText = escapedText.replace("\n", "\\n");
            escapedText = escapedText.replace("\r", "\\r");
            escapedText = escapedText.replace("\t", "\\t");
            escapedText = escapedText.replace("\b", "\\b");
            escapedText = escapedText.replace("\f", "\\f");
            return escapedText;
        }

        public static boolean contains(String[] things, String string){
            for (String thing: things) {
                if(string.contains(thing)){
                    return true;
                }
            }
            return false;
        }

        public static int has(String[] things, String string){
            for(int i = 0; i < things.length; i++){
                if(string.contains(things[i])){
                    return i;
                }
            }
            return -1;
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
        }

        public static class detector {

            public static Matcher findPhoneNumbers(String text){
                String regex = "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b";
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
                }else if(src.startsWith("www")){
                    src = "https://" + src;
                }else if(src.startsWith("./")){
                    src = url + "/" + src;
                    return src.replaceFirst("\\./", "");
                }
                return src;
            }

            public static boolean isWebpage(String uri){
                String[] fileExts = { ".jpg", ".jpeg", ".png", ".gif", ".md", ".txt" };
                String lowercaseUri = uri.toLowerCase();
                if (lowercaseUri.startsWith("https://") || lowercaseUri.startsWith("http://") || lowercaseUri.startsWith("www")) {
                    for (String extension : fileExts) {
                        if (lowercaseUri.endsWith(extension)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }

            public static boolean isFile(String uri){
                return uri.startsWith("file://") || uri.startsWith("c:/") || uri.startsWith("\\");
            }

            public static String getType(String uri){
                if(isWebpage(uri) || uri.endsWith("html")){
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

            public static boolean isValidLink(String link) {
                if(link.contains("cart") || link.contains("search") || link.contains("account") || link.contains("#")){ //Audit out pages
                    return false;
                }
                return !link.startsWith("tel:") || !link.startsWith("mailto:") || !link.startsWith("sms:") || !link.startsWith("skype:") || !link.startsWith("#");
            }
        }
    }
}
