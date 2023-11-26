package com.telifie.Models.Connectors;

import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Utilities.Packages;
import com.telifie.Models.Utilities.Parameters;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;

public class OpenWeatherMap {

    public static String get(Parameters params){
        String apiKey = Packages.get("com.telifie.connectors.openweathermap").getAccess();
        double latitude = params.getLatitude();
        double longitude = params.getLongitude();
        String apiUrl = "https://api.openweathermap.org/data/2.5/forecast?units=imperial&exclude=hourly,minutely,current&lat=" + latitude + "&lon=" + longitude + "&appid=" + apiKey;
        try {
            Document doc = Jsoup.connect(apiUrl).ignoreContentType(true).get();
            return Andromeda.tools.escape(doc.text());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
