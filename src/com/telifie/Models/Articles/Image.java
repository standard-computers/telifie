package com.telifie.Models.Articles;

import java.io.Serializable;

import org.bson.Document;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class Image implements Serializable {

    private String url, caption, source;

    public Image(String url, String caption, String source) {
        this.url = url;
        this.caption = caption;
        this.source = source;
    }

    public Image(Document document) throws NullPointerException {
        this.url = document.getString("url");
        this.caption = document.getString("caption");
        this.source = document.getString("source");
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void identify(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // Load the image file
        Mat image = Imgcodecs.imread("image.jpg");
        if (image.empty()) {
            System.out.println("Error: Could not load image");
        }else{

        }
    }

    @Override
    public String toString() {
        return "{" +
                (url == null || url.equals("null") ? "" : "\"url\" : \"" + url + "\",") +
                (caption == null || caption.equals("null") ? "" : "\"caption\" : \"" + caption + "\",") +
                (source == null || source.equals("null") ? "" : "\"source\" : \"" + source + '\"') +
                '}';
    }
}
