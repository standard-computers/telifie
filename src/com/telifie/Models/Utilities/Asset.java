package com.telifie.Models.Utilities;

import com.telifie.Models.Utilities.Network.Network;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Asset {

    public final String uri;
    private String localUri;
    private String contents;
    private long length;
    private int[] dimensions;

    public Asset(String uri){
        this(uri, false);
    }

    public Asset(String uri, boolean doContents){
        this.uri = uri;
        if((uri.startsWith("https://") || uri.startsWith("http://") || uri.startsWith("www")) && !isWebpage()){
            try {
                analyseFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(doContents){
            download();
            getContents();
        }
    }

    private void analyseFile() throws IOException {
        URL url = Network.url(this.uri);
        URLConnection connection = url.openConnection();
        connection.connect();
        length = connection.getContentLength();
        if(getType(this.uri).equals("image")){
            try {
                BufferedImage image = ImageIO.read(url);
                if(image != null){
                    this.dimensions = new int[]{image.getWidth(), image.getHeight()};
                }else{
                    this.dimensions = new int[]{0,0};
                }
            } catch (IOException e) {
                this.dimensions = new int[]{0,0};
            }
        }
    }

    public int[] getDimensions(){
        return this.dimensions;
    }

    public void download(){
        try {
            URL url = Network.url(uri);
            localUri = Telifie.configDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1];
            InputStream inputStream = url.openStream();
            Files.copy(inputStream, Paths.get(localUri), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.error("FAILED TO DOWNLOAD : " + uri, "A0x076");
        }
    }

    public String getContents(){
        if(contents != null){
            return contents;
        }
        try {
            contents = new String(Files.readAllBytes(Paths.get(localUri)));
            return contents;
        } catch (IOException e) {
            return "";
        }
    }

    public String fileSize(){
        if (this.length <= 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(this.length) / Math.log10(1024));
        double fileSize = this.length / Math.pow(1024, digitGroups);
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        return decimalFormat.format(fileSize) + " " + units[digitGroups];
    }

    public String getExt(){
        int ldi = uri.lastIndexOf('.');
        if (ldi >= 0) {
            return uri.substring(ldi + 1);
        } else {
            return "";
        }
    }

    public String lineCount(){
        return String.valueOf(contents.split("\n").length);
    }

    public String wordCount(){
        return String.valueOf(contents.split(" ").length);
    }

    public boolean isWebpage(){
        String lowercaseUri = uri.toLowerCase();
        if (lowercaseUri.startsWith("https://") || lowercaseUri.startsWith("http://") || lowercaseUri.startsWith("www")) {
            return !Telifie.tools.contains(new String[]{ ".jpg", ".jpeg", ".png", ".gif", ".md", ".txt" }, lowercaseUri);
        }
        return false;
    }

    public static String getType(String uri){
        uri = uri.split("\\?")[0].toLowerCase();
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

    public static boolean isWebpage(String uri){
        String lowercaseUri = uri.toLowerCase();
        if (lowercaseUri.startsWith("https://") || lowercaseUri.startsWith("http://") || lowercaseUri.startsWith("www")) {
            return !Telifie.tools.contains(new String[]{ ".jpg", ".jpeg", ".png", ".gif", ".md", ".txt" }, lowercaseUri);
        }
        return false;
    }

    public static boolean isValidLink(String link) {
        if(Telifie.tools.contains(new String[]{"/error/", "#", "mailto:", ".xml", "tel:", "sms:", "skype:", "robots.txt"}, link) || containsIPAddress(link) || link.startsWith("http://")) {
            return false;
        }
        return true;
    }

    private static boolean containsIPAddress(String input) {
        String ipv4Pattern = "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
                + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
                + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
                + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
        String ipv6Pattern = "([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|"
                + "([0-9a-fA-F]{1,4}:){1,7}:|"
                + "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|"
                + "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|"
                + "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|"
                + "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|"
                + "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|"
                + "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|"
                + ":((:[0-9a-fA-F]{1,4}){1,7}|:)|"
                + "fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|"
                + "::(ffff(:0{1,4}){0,1}:){0,1}"
                + "((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}"
                + "(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|"
                + "([0-9a-fA-F]{1,4}:){1,4}:"
                + "((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}"
                + "(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])";
        String ipAddressPattern = ipv4Pattern + "|" + ipv6Pattern;
        Pattern pattern = Pattern.compile(ipAddressPattern);
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }
}