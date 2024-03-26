package com.telifie.Models.Utilities.Network;

import com.telifie.Models.Utilities.Asset;
import org.jsoup.select.Elements;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Network {

    public static URL url(String uri){
        try {
            return new URL(uri);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decode(String uri){
        return URLDecoder.decode(uri, StandardCharsets.UTF_8);
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

    public static ArrayList<String> extractLinks(Elements elements, String root){
        ArrayList<String> links = new ArrayList<>();
        elements.forEach(el -> {
            String fxd = Network.fixLink(root, el.attr("href"));
            if(Asset.isValidLink(fxd)){
                links.add(fxd);
            }
        });
        return links;
    }
}