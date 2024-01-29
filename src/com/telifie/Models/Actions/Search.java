package com.telifie.Models.Actions;

import com.mongodb.client.model.Filters;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Unit;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Connectors.Radar;
import com.telifie.Models.Connectors.Rest;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.Packages;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Search {

    private Result result;

    public Result execute(Session session, String q, Parameters params){
        ArticlesClient articles = new ArticlesClient(session);
        Unit query = new Unit(q);
        Document filter = filter(query, params);
        Bson sf = Filters.ne("description", "Image");
        if(params.getIndex().equals("images")){
            sf = Filters.eq("description", "Image");
        }else if(params.getIndex().equals("locations")){
            sf = Filters.exists("location");
        }
        filter = new Document("$and", Arrays.asList(sf, Filters.or(filter)));
        ArrayList<Article> results = articles.search(query, params, filter);
        ArrayList<Article> paged = paginate(results, params.getPage(), params.getResultsPerPage());
        result = new Result(query.text(), params, "articles", paged);
        result.setTotal(results.size());
        result.setGenerated(this.generated(q, params));
        return result;
    }

    private Document filter(Unit q, Parameters params){

        if(q.text().startsWith("@")){

            String p = q.text().split(" ")[0].replace("@","");
            String spl = q.text().replace("@" + p, "");
            return new Document(p, Pattern.compile("\\b" + Pattern.quote(spl.trim()) + "\\b", Pattern.CASE_INSENSITIVE));
        }else if(q.text().matches("^attribute\\s*:\\s*.*")){

            String[] spl = q.text().split(":");
            if(spl.length >= 2){
                String[] spl2 = spl[1].split("=");
                String key = spl2[0].trim().toLowerCase();
                if(spl2.length >= 2){
                    String value = spl2[1].trim().toLowerCase();
                    return new Document("attributes", new Document("$elemMatch", new Document("key", Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE)).append("value", Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE))));
                }
                return new Document("attributes.key", Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE));
            }
        }else if(q.text().endsWith("near me")){

            String me = q.text().replace("near me", "").trim();
            return new Document("$and", Arrays.asList(
                new Document("$or", Arrays.asList( new Document("tags", pattern(me)),new Document("description", pattern(me)), new Document("title", pattern(me)) )),
                new Document("location", new Document("$near", new Document("$geometry", new Document("type", "Point").append("coordinates", Arrays.asList( params.getLongitude(), params.getLatitude() )) ).append("$maxDistance", 16000) )
            )));
        }else if(q.contains(Andromeda.taxon("proximity"))){
            String splr = q.get(Andromeda.taxon("proximity"));
            params.setIndex("locations");
            String[] spl = q.text().split(splr);
            if(spl.length >= 2){
                String subject = spl[0].trim();
                String place = spl[1].trim();
                Article pl = new ArticlesClient(new Session("", "telifie")).findPlace(place, params);
                params.setLatitude( Double.parseDouble(pl.getAttribute("Longitude")));
                params.setLongitude(Double.parseDouble(pl.getAttribute("Latitude")));
                return new Document("$and", Arrays.asList(
                    new Document("$or", Arrays.asList(
                            new Document("tags", pattern(subject)),
                            new Document("description", pattern(subject)),
                            new Document("title", pattern(subject))
                    )), new Document("location", new Document("$near", new Document("$geometry", new Document("type", "Point").append("coordinates", Arrays.asList(Double.parseDouble(pl.getAttribute("Longitude")), Double.parseDouble(pl.getAttribute("Latitude")))) ).append("$maxDistance", 16000)))
                ));
            }
        }
        ArrayList<Document> or = new ArrayList<>();
        for (String word : q.cleanedTokens()) {
            or.add(new Document("link", pattern(word)));
            or.add(new Document("title", pattern(word)));
            or.add(new Document("description", pattern(word)));
        }
        or.add(new Document("tags", new Document("$in", Collections.singletonList(q.text()))));
        return new Document("$or", or);
    }

    private String generated(String q, Parameters params){
        if(params.getPage() == 1){
            Unit u = new Unit(q);
            if(u.text().contains("*") || u.text().contains("+") || u.text().contains("-") || u.text().contains("/") || Andromeda.tools.contains(Andromeda.NUMERALS, q)){
                String mathExpressionPattern = "[\\d\\s()+\\-*/=xX^]+";
                Pattern pattern = Pattern.compile(mathExpressionPattern);
                Matcher matcher = pattern.matcher(u.text());
                while (matcher.find()) {
                    String match = matcher.group().trim();
                    result.setSource("com.telifie.connectors.wolfram");
                    return Rest.get(Packages.get("com.telifie.connectors.wolfram"), new HashMap<>() {{
                        put("i", match);
                        put("appid", Packages.get("com.telifie.connectors.wolfram").getAccess());
                    }});
                }
            }else if(q.contains("uuid")){

                return "Here's a UUID  \\n" + UUID.randomUUID();
            }else if(q.contains("weather")){

                result.setSource("com.telifie.connectors.openweathermap");
                return Andromeda.tools.escape(Rest.get(Packages.get("com.telifie.connectors.openweathermap"), new HashMap<>() {{
                    put("units", "imperial");
                    put("excluded", "hourly,minutely,current");
                    put("lat", String.valueOf(params.getLatitude()));
                    put("lon", String.valueOf(params.getLongitude()));
                    put("appid", Packages.get("com.telifie.connectors.openweathermap").getAccess());
                }}));
            }else if(q.contains("flip a coin")){

                int random = new Random().nextInt(2);
                return ((random == 0) ? "Heads" : "Tails");
            }else if(q.contains("roll") && q.contains("dice")){

                int random = new Random().nextInt(6) + 1;
                return "Your dice roll is " + random;
            }else if(q.contains("random") && q.contains("color")){

                Color c = new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256));
                String hex = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
                String rgb = String.format("RGB(%d, %d, %d)", c.getRed(), c.getGreen(), c.getBlue());
                return "Here is a random color: Hex is " + hex + " and RGB " + rgb;
            }else if(q.matches("\\b\\d+\\s+([A-Za-z0-9.\\-\\'\\s]+)\\s+" + // Street number and name
                    "(St\\.?|Street|Rd\\.?|Road|Ave\\.?|Avenue|Blvd\\.?|Boulevard|Ln\\.?|Lane|Dr\\.?|Drive|Ct\\.?|Court)\\s+" + // Street type
                    "(\\w+),\\s+" + // City
                    "(Ohio|OH|Ala|AL|Alaska|AK|Ariz|AZ|Ark|AR|Calif|CA|Colo|CO|Conn|CT|Del|DE|Fla|FL|Ga|GA|Hawaii|HI|Idaho|ID|Ill|IL|Ind|IN|Iowa|IA|Kans|KS|Ky|KY|La|LA|Maine|ME|Md|MD|Mass|MA|Mich|MI|Minn|MN|Miss|MS|Mo|MO|Mont|MT|Nebr|NE|Nev|NV|N\\.H\\.|NH|N\\.J\\.|NJ|N\\.M\\.|NM|N\\.Y\\.|NY|N\\.C\\.|NC|N\\.D\\.|ND|Okla|OK|Ore|OR|Pa|PA|R\\.I\\.|RI|S\\.C\\.|SC|S\\.D\\.|SD|Tenn|TN|Tex|TX|Utah|UT|Vt|VT|Va|VA|Wash|WA|W\\.Va|WV|Wis|WI|Wyo|WY)\\s+" + // State
                    "(\\d{5}(?:[-\\s]\\d{4})?)")){

                try {
                    Radar.get(q);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                //TODO map/radar lookup
            }else if(u.startsWith(Andromeda.taxon("interrogative"))){


            }else if(u.startsWith(Andromeda.taxon("verb"))){


            }
        }
        return "";
    }

    private ArrayList<Article> paginate(ArrayList<Article> results, int page, int pageSize) {
        if(results.size() < pageSize){
            return results;
        }
        ArrayList<Article> paginatedResults = new ArrayList<>();
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, results.size());
        if (startIndex < results.size()) {
            paginatedResults.addAll(results.subList(startIndex, endIndex));
        }
        return paginatedResults;
    }

    public static Pattern pattern(String value){
        return Pattern.compile("\\b" + Pattern.quote(value) + "\\b", Pattern.CASE_INSENSITIVE);
    }
}