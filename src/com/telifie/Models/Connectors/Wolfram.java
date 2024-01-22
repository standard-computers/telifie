package com.telifie.Models.Connectors;

import com.telifie.Models.Utilities.Packages;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Wolfram {

    public static String compute(String q){
        String apiKey = Packages.get("com.telifie.connectors.wolfram").getAccess();
        String apiUrl = null;
        try {
            apiUrl = "https://api.wolframalpha.com/v1/result?i=" + URLEncoder.encode(q, "UTF-8") + "&appid=" + apiKey;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        try {
            Document doc = Jsoup.connect(apiUrl).ignoreContentType(true).get();
            return doc.text();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}