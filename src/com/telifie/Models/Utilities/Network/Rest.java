package com.telifie.Models.Utilities.Network;

import com.telifie.Models.Utilities.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.util.Map;
import com.telifie.Models.Utilities.Package;

public class Rest {

    public static String get(Package p, Map<String, String> params){
        StringBuilder req = new StringBuilder(p.getEndpoint("endpoint") + "?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            req.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        if (req.length() > 0) {
            req.setLength(req.length() - 1);
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