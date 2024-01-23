package com.telifie.Models.Connectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import com.telifie.Models.Utilities.Package;

public class Rest {

    public static String get(Package p, Map<String, String> params){
        String req = p.getUrl("endpoint")+ "?";
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                req += entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8") + "&";
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        try {
            Document doc = Jsoup.connect(req).ignoreContentType(true).get();
            return doc.text();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}