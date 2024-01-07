package com.telifie.Models.Actions;

import com.mongodb.client.model.Filters;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Encoder;
import com.telifie.Models.Andromeda.Unit;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Connectors.OpenWeatherMap;
import com.telifie.Models.Connectors.Radar;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.Packages;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.License;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class Search {

    private Result result;

    public Result execute(Session session, String q, Parameters params){
        String cleanedQuery = Encoder.clean(q);
        q = q.toLowerCase().trim();
        ArticlesClient articles = new ArticlesClient(session);
        Document filter = filter(q, cleanedQuery, params);
        Bson sf = Filters.ne("description", "Image");
        if(params.getIndex().equals("images")){
            sf = Filters.eq("description", "Image");
        }else if(params.getIndex().equals("locations")){
            sf = Filters.exists("location");
        }
        filter = new Document("$and", Arrays.asList( sf, Filters.or(filter) ));
        ArrayList<Article> results = articles.search(q, cleanedQuery, params, filter, params.isQuickResults());
        ArrayList<Article> paged = paginateArticles(results, params.getPage(), params.getResultsPerPage());
        result = new Result(q, params, "articles", paged);
        result.setTotal(results.size());
        result.setGenerated(this.generated(q, params));
        return result;
    }

    private Document filter(String query, String cleanedQuery, Parameters params){

        if(query.matches("^description\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2) {
                return new Document("description", Pattern.compile("\\b" + Pattern.quote(spl[1].trim()) + "\\b", Pattern.CASE_INSENSITIVE));
            }
        }else if(query.matches("^attribute\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2){
                String[] spl2 = spl[1].split("=");
                String key = spl2[0].trim().toLowerCase();
                if(spl2.length >= 2){
                    String value = spl2[1].trim().toLowerCase();
                    return new Document("attributes", new Document("$elemMatch", new Document("key", Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE))
                            .append("value", Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE))));
                }
                return new Document("attributes.key", Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE));
            }
        }else if(query.startsWith("define ") || query.startsWith("title ")) {

            return new Document("title", pattern(query.replaceFirst("^(define|title)", "").trim()));
        }else if(query.matches("^(\\d+)\\s+([A-Za-z\\s]+),\\s+([A-Za-z\\s]+),\\s+([A-Za-z]{2})\\s+(\\d{5})$")){

            return new Document("$and", Arrays.asList(new Document("attribute.key", "Address"), new Document("attribute.value", pattern(query))));
        }else if(query.matches("^\\+\\d{1,3}\\s*\\(\\d{1,3}\\)\\s*\\d{3}-\\d{4}$")){

            return new Document("$and", Arrays.asList( new Document("attribute.key", "Phone"), new Document("attribute.value", query) ) );
        }else if (query.matches("^\\w+@\\w+\\.[a-zA-Z]{2,3}$")) {

            return new Document("$and", Arrays.asList( new Document("attribute.key", "Email"), new Document("attribute.value", query.toLowerCase()) ));
        }else if(query.matches("\\b(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{2}-[a-zA-Z]{3}-\\d{4}|[a-zA-Z]+ \\d{1,2}, \\d{4})\\b")){

            return new Document("attribute.value", query);
        }else if(query.endsWith("near me")){

            String q = query.replace("near me", "").trim();
            return new Document("$and", Arrays.asList(
                    new Document("$or", Arrays.asList(
                            new Document("tags", pattern(q)),
                            new Document("description", pattern(q)),
                            new Document("title", pattern(q))
                    )),
                    new Document("location", new Document("$near",
                            new Document("$geometry", new Document("type", "Point")
                                .append("coordinates", Arrays.asList( params.getLongitude(), params.getLatitude() ))
                            ).append("$maxDistance", 16000)
                    )
                )
            ));
        }else if(Andromeda.tools.has(Andromeda.PROXIMITY, query) > -1){
            String splr = Andromeda.PROXIMITY[Andromeda.tools.has(Andromeda.PROXIMITY, query)];
            params.setIndex("locations");
            String[] spl = query.split(splr);
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
                        )),
                        new Document("location", new Document("$near",
                                new Document("$geometry", new Document("type", "Point")
                                        .append("coordinates", Arrays.asList(Double.parseDouble(pl.getAttribute("Longitude")), Double.parseDouble(pl.getAttribute("Latitude"))))
                                ).append("$maxDistance", 16000)
                        )
                        )
                ));
            }
        }else if((query.contains("*") || query.contains("/") || query.contains("+") || query.contains("-")) && Andromeda.tools.contains(Andromeda.NUMERALS, query)){
            return  new Document("description", "Calculator");
        }
        String[] exploded = cleanedQuery.split("\\s+");
        ArrayList<Document> or = new ArrayList<>();
        or.add(new Document("title", pattern(query)));
        or.add(new Document("link", pattern(query)));
        for (String word : exploded) {
            or.add(new Document("title", pattern(word)));
            or.add(new Document("description", pattern(word)));
        }
        or.add(new Document("tags", new Document("$in", Collections.singletonList(query))));
        return new Document("$or", or);
    }

    private String generated(String q, Parameters params){
        if(params.getPage() == 1){
            Unit u = new Unit(q);
            if((q.contains("*") || q.contains("/") || q.contains("+") || q.contains("-")) && Andromeda.tools.contains(Andromeda.NUMERALS, q)){
                License.iConfirmNonCommercialUse("Telifie LLC");
                Expression expression = new Expression(q);
                if (expression.checkSyntax()) {
                    return String.valueOf(expression.calculate());
                }
            }else if(q.contains("uuid")){
                return "Here's a UUID  \\n" + UUID.randomUUID();
            }else if(q.contains("weather")){
                result.setSource(Andromeda.tools.escape(Packages.get("com.telifie.connectors.openweathermap").toString()));
                return OpenWeatherMap.get(params);
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
            }else if(q.matches("\\b\\d+\\s+([A-Za-z0-9\\.\\-\\'\\s]+)\\s+" + // Street number and name
                    "(St\\.?|Street|Rd\\.?|Road|Ave\\.?|Avenue|Blvd\\.?|Boulevard|Ln\\.?|Lane|Dr\\.?|Drive|Ct\\.?|Court)\\s+" + // Street type
                    "(\\w+),\\s+" + // City
                    "(Ohio|OH|Ala|AL|Alaska|AK|Ariz|AZ|Ark|AR|Calif|CA|Colo|CO|Conn|CT|Del|DE|Fla|FL|Ga|GA|Hawaii|HI|Idaho|ID|Ill|IL|Ind|IN|Iowa|IA|Kans|KS|Ky|KY|La|LA|Maine|ME|Md|MD|Mass|MA|Mich|MI|Minn|MN|Miss|MS|Mo|MO|Mont|MT|Nebr|NE|Nev|NV|N\\.H\\.|NH|N\\.J\\.|NJ|N\\.M\\.|NM|N\\.Y\\.|NY|N\\.C\\.|NC|N\\.D\\.|ND|Okla|OK|Ore|OR|Pa|PA|R\\.I\\.|RI|S\\.C\\.|SC|S\\.D\\.|SD|Tenn|TN|Tex|TX|Utah|UT|Vt|VT|Va|VA|Wash|WA|W\\.Va|WV|Wis|WI|Wyo|WY)\\s+" + // State
                    "(\\d{5}(?:[-\\s]\\d{4})?)")){

                try {
                    Radar.get(q);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //TODO map/radar lookup
            }else if(u.startsWith("interrogative")){
                //TODO find subject and inquiry
            }
        }
        return "";
    }

    private ArrayList<Article> paginateArticles(ArrayList<Article> results, int page, int pageSize) {
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