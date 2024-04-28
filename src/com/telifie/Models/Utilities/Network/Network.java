package com.telifie.Models.Utilities.Network;

import java.net.MalformedURLException;
import java.net.URL;

public class Network {

    public static URL url(String uri){
        try {
            return new URL(uri);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String fixLink(String url, String src){
        try {
            URL baseURL = new URL(url);
            URL srcURL = new URL(baseURL, src);
            return srcURL.toString();
        } catch (MalformedURLException e) {
            return src;
        }
    }
}