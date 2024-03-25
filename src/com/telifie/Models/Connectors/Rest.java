package com.telifie.Models.Connectors;

import com.telifie.Models.Utilities.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.telifie.Models.Utilities.Package;

public class Rest {

    public static String get(Package p, Map<String, String> params){
        StringBuilder req = new StringBuilder(p.getEndpoint("endpoint") + "?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            req.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).append("&");
        }
        try {
            Log.flag("Executing REST request -> " + p.getName(), "NETx001");
            Document doc = Jsoup.connect(req.toString()).ignoreContentType(true).get();
            return doc.text();
        } catch (IOException e) {
            Log.error("Failed REST request -> " + p.getName(), "NETx101");
        }
        return "";
    }
}