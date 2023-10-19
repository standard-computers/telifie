package com.telifie.Models.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;

public class Asset {

    private String uri, localUri;
    private File f;
    private String contents;

    public Asset(String uri){
        this.uri = uri;
        f = new File(uri);
        if(isURL() && !isWebpage()){
            download();
            getContents();
        }
    }

    public String download(){
        try {
            URL url = new URL(uri);
            localUri = Telifie.configDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1];
            InputStream inputStream = url.openStream();
            Files.copy(inputStream, Paths.get(localUri), StandardCopyOption.REPLACE_EXISTING);
            return localUri;
        } catch (IOException e) {
            return "";
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
        long fs = f.length();
        if (fs <= 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(fs) / Math.log10(1024));
        double fileSize = fs / Math.pow(1024, digitGroups);
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

    public boolean isFile(){
        return uri.startsWith("file://") || uri.startsWith("c:/") || uri.startsWith("\\");
    }

    public static boolean isFile(String uri){
        return uri.startsWith("file://") || uri.startsWith("c:/") || uri.startsWith("\\");
    }

    public boolean isURL(){
        return uri.startsWith("https://") || uri.startsWith("http://") || uri.startsWith("www");
    }

    public boolean isWebpage(){
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

    public static boolean isValidLink(String link) {
        if(link.contains("cart") || link.contains("search") || link.contains("account") || link.contains("#")){ //Audit out pages
            return false;
        }
        return !link.startsWith("tel:") || !link.startsWith("mailto:") || !link.startsWith("sms:") || !link.startsWith("skype:") || !link.startsWith("#");
    }
}
