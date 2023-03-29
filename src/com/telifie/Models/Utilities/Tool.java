package com.telifie.Models.Utilities;

import com.telifie.Models.Actions.Out;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tool {

    private static final String[] ALPHAS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private static final String[] NUMERALS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

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

    public static boolean isUrl(String uri){

        return uri.startsWith("https://") || uri.startsWith("http://") || uri.startsWith("www");
    }

    public static boolean isFile(String uri){

        return uri.startsWith("file://") || uri.startsWith("c:/") || uri.startsWith("\\");
    }

    public static String getExtension(String uri){
        return uri.split("\\.")[uri.split("\\.").length - 1];
    }

    public static String getType(String uri){
        if(Tool.isUrl(uri) || uri.endsWith("html")){
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

    public static boolean equals(char sample, char[] chars){

        for(char ch : chars){

            if(sample == ch){

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

    public static boolean isValidLink(String link, String uri) {
        if(link.contains("cart") || link.contains("search") || link.contains("account")){ //Audit out pages
            return false;
        }

        // Check if the URI has a valid scheme and host, or if it's a relative link
        if (link.startsWith("tel:")
                && link.startsWith("mailto:")
                && link.startsWith("sms:")
                && link.startsWith("skype:")
                && link.startsWith("#")) {
            return false;
        }

        return true;
    }

    public static String escape(String string){
        return  StringEscapeUtils.escapeJson(string);
    }

    public static String extractBodyContent(String html) {
        // Remove script tags
        String bodyHtml = html.replaceAll("<script.*?</script>", "");

        // Remove comments
        bodyHtml = bodyHtml.replaceAll("<!--.*?-->", "");
        bodyHtml = bodyHtml.replaceAll("<label.*?</label>", "");
        bodyHtml = bodyHtml.replaceAll("<form.*?</form>", "");
        bodyHtml = bodyHtml.replaceAll("<label.*?</label>", "");
        bodyHtml = bodyHtml.replaceAll("<input.*?>", "");

        // Extract content of the body element
        Pattern pattern = Pattern.compile("<body>(.*?)</body>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(bodyHtml);
        if (matcher.find()) {
            String bodyContent = matcher.group(1);
            return bodyContent;
        } else {
            return "";
        }
    }

    public static String convertHtmlToMarkdown(String html) {
        // Replace HTML tags with Markdown syntax
        String markdown = html.replaceAll("<h1>(.*?)</h1>", "# $1\n")
                .replaceAll("<h2>(.*?)</h2>", "## $1\n")
                .replaceAll("<h3>(.*?)</h3>", "### $1\n")
                .replaceAll("<h4>(.*?)</h4>", "#### $1\n")
                .replaceAll("<h5>(.*?)</h5>", "##### $1\n")
                .replaceAll("<h6>(.*?)</h6>", "###### $1\n")
                .replaceAll("<p>(.*?)</p>", "$1\n\n")
                .replaceAll("<ul>(.*?)</ul>", "$1\n")
                .replaceAll("<li>(.*?)</li>", "* $1\n");

        return markdown;
    }

    public static ArrayList<String> extractLinks(Elements elements, String root){
        ArrayList<String> links = new ArrayList<>();
        for(Element el : elements){
             String link = el.attr("href");
            if(!link.equals("/") && !link.equals("") & !link.equals(root) && !Tool.containsAnyOf(new String[]{"facebook", "instagram", "spotify", "linkedin", "youtube"}, link)){
                String fixed = Tool.fixLink(root, link);
                if(Tool.isValidLink(fixed, root)){

                    links.add(fixed);
                }
            }
        }
        return links;
    }
}
