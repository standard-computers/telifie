package com.telifie.Models.Utilities;

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
        if(src.startsWith("//")){ //SRC needs protocol
            return "https:" + src.trim();
        }else if(src.startsWith("/")){ //SRC is subdirectory of parent URL
            if(url.endsWith("/")) {
                return url.substring(0, url.length() - 1) + src;
            }
            return url + src;
        }else if(src.startsWith("www")){ //SRC needs protocol
            return "https://" + src;
        }else if(src.startsWith("./")){
            return (url + "/" + src).replaceFirst("\\./", "");
        }
        return src;
    }
}
