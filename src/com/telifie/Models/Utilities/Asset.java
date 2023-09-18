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
}
