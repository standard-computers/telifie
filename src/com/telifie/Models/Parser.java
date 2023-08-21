package com.telifie.Models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.telifie.Models.Actions.Event;
import com.telifie.Models.Articles.*;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.TimelinesClient;
import com.telifie.Models.Utilities.*;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;

public class Parser {

    private static String uri, host;
    private static final ArrayList<Article> traversable = new ArrayList<>();
    private static final ArrayList<String> parsed = new ArrayList<>();
    private static ArticlesClient articles;

    public Parser(Configuration config){
        articles = new ArticlesClient(config);
    }

    public static class engines {

        public static Article parse(String uri){
            traversable.removeAll(traversable);
            Parser.uri = uri;
            if(Telifie.tools.detector.isUrl(uri)){
                try {
                    host = new URL(uri).getHost();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                return Parser.engines.website(uri);
            }else if(Telifie.tools.detector.isFile(uri)){
                File file = new File(uri);
                if(file.exists()){

                }
            }
            return null;
        }

        public static void recursive(Configuration config, int start){
            articles = new ArticlesClient(config);
            TimelinesClient timelines = new TimelinesClient(config);
            ArrayList<Article> as = articles.linked();
            List<Article> as2 = as.subList(start, as.size());
            for(Article a : as2){
                parsed.removeAll(parsed);
                int lastCrawl = timelines.lastEvent(a.getId(), Event.Type.CRAWL);
                if(lastCrawl > 2592000 || lastCrawl == -1){ //7 days
                    timelines.addEvent(a.getId(), new Event(
                            Event.Type.CRAWL,
                            "com.telifie.web-app@parser",
                            "Crawled")
                    );
                    parsed.add(a.getLink());
                    Parser.engines.crawl(a.getLink(), Integer.MAX_VALUE, false);
                }
            }
        }

        public static Article crawl(String uri, int limit, boolean allowExternalCrawl){
            traversable.removeAll(traversable);
            Parser.uri = uri;
            try {
                host = new URL(uri).getHost();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            return Parser.engines.fetch(uri, limit, allowExternalCrawl);
        }

        protected static Article fetch(String url, int limit, boolean allowExternalCrawl){
            try {
                URL urlObj = new URL(url);
                String host = urlObj.getProtocol() + "://" + urlObj.getHost();
                RobotPermission robotPermission = new RobotPermission(host);
                if(!robotPermission.isAllowed(urlObj.getPath())){
                    System.out.println("Disallowed by robots.txt: " + url);
                    return null;
                }
                Connection.Response response = Jsoup.connect(url).userAgent("telifie/1.0").execute();
                if(response.statusCode() == 200){
                    Document root = response.parse();
                    Article article = webpage.extract(url, root);
                    Parser.traversable.add(article);
                    boolean created = true;
                    if(article.getSource() != null && !articles.existsWithSource(article.getSource().getUrl())){
                        created = articles.create(article);
                        System.out.println("With source -> Created article " + article.getTitle());
                    }else if(article != null && article.getSource() == null && (created = articles.create(article))){
                        System.out.println("With link -> Created article " + article.getTitle());
                    }
                    ArrayList<Element> links = root.getElementsByTag("a");
                    for(Element link : links){
                        String href = Telifie.tools.detector.fixLink(host, link.attr("href").split("\\?")[0]);
                        if(!isParsed(href)
                                && Telifie.tools.detector.isUrl(href)
                                && !Telifie.tools.strings.contains(new String[]{
                                "facebook.com", "instagram.com", "spotify.com",
                                "linkedin.com", "youtube.com", "pinterest.com",
                                "twitter.com", "tumblr.com", "reddit.com"}, href)
                        ) {
                            if((allowExternalCrawl && !href.contains(host)) || (!allowExternalCrawl && href.contains(host))){
                                System.out.println("Fetching (" + parsed.size() + ")" + href);
                                parsed.add(href);
                                if(parsed.size() >= limit){
                                    return article;
                                }
                                try {
                                    System.out.println("awaiting");
                                    Thread.sleep(3000);
                                    fetch(href, limit, allowExternalCrawl);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    return article;
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        }

        public static Article website(String url){
            parsed.add(url);
            try {
                Connection.Response response = Jsoup.connect(url).userAgent("telifie/1.0").execute();
                if(response.statusCode() == 200){
                    Document root = response.parse();
                    Article article = webpage.extract(url, root);
                    ArrayList<String> links = Telifie.tools.make.extractLinks(root.getElementsByTag("a"), uri);
                    if(links.size() > 0){
                        for(String link : links){
                            if(Telifie.tools.strings.contains(new String[]{"facebook.com", "instagram.com", "spotify.com", "linkedin.com", "youtube.com", "pinterest.com", "github.com", "twitter.com", "tumblr.com", "reddit.com"}, link)){
                                URI uri = null;
                                try {
                                    uri = new URI(link);
                                    String domain = uri.getHost();
                                    String k = (domain.split("\\.")[0].equals("www") ? domain.split("\\.")[1] : domain.split("\\.")[0]);
                                    k = k.substring(0, 1).toUpperCase() + k.substring(1);
                                    String[] l = link.split("\\?")[0].split("/");
                                    String un = l[l.length - 1].trim();
                                    if(!un.equals("watch")){
                                        article.addAttribute(new Attribute(k, un));
                                    }
                                } catch (URISyntaxException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                    if(article.getLink() == null || article.getLink().contains(new URL(uri).getHost())) {
                        Parser.traversable.add(article);
                    }
                    return article;
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        }

        /**
         * Parses articles for csv rows
         * Provide csv file location. <a href=''>See template</a>
         * @param uri Location of csv on disk
         * @param withPriority priority of all articles in batch
         * @return ArrayList<Article> List of Articles
         */
        public static ArrayList<Article> batch(String uri, Double withPriority){

            if(uri.endsWith("csv")) {
                try {
                    URL url = new URL(uri);
                    InputStream inputStream = url.openStream();
                    Files.copy(inputStream, Paths.get(Telifie.getConfigDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1]), StandardCopyOption.REPLACE_EXISTING);
                    ArrayList<String[]> lines = new ArrayList<>();
                    try (CSVReader reader = new CSVReader(new FileReader(Telifie.getConfigDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1]))) {
                        String[] fields;
                        while ((fields = reader.readNext()) != null) {
                            lines.add(fields);
                        }
                    } catch (IOException | CsvException e) {
                        e.printStackTrace();
                    }
                    ArrayList<Article> articles = new ArrayList<>();
                    String[] headers = lines.get(0);
                    int titleIndex = -1, descriptionIndex = -1, iconIndex = -1, linkIndex = -1, contentIndex = -1, tagsIndex = -1;
                    for (int i = 0; i < headers.length; i++) {
                        String hV = headers[i].trim().toLowerCase();
                        switch (hV) {
                            case "title" -> titleIndex = i;
                            case "description" -> descriptionIndex = i;
                            case "link" -> linkIndex = i;
                            case "content" -> contentIndex = i;
                            case "icon" -> iconIndex = i;
                            case "tags" -> tagsIndex = i;
                        }
                    }
                    String batchId = Telifie.tools.make.shortEid().toLowerCase();
                    for (int i = 1; i < lines.size(); i++) {
                        String[] articleData = lines.get(i);
                        Article article = new Article();
                        article.setPriority(withPriority);
                        article.addAttribute(new Attribute("*batch", batchId));
                        for (int g = 0; g < articleData.length; g++) {
                            String value = articleData[g];
                            if (g == titleIndex && titleIndex > -1) {
                                article.setTitle(value);
                            } else if (g == descriptionIndex && descriptionIndex > -1) {
                                article.setDescription(value);
                            } else if (g == linkIndex && linkIndex > -1) {
                                article.setLink(value);
                            } else if (g == contentIndex && contentIndex > -1) {
                                article.setContent(value);
                            } else if(g == iconIndex && iconIndex > -1){
                                article.setIcon(value);
                            } else if(g == tagsIndex){
                                String[] tags = value.split(",");
                                for(String tag : tags){
                                    article.addTag(tag.toLowerCase().trim());
                                }
                            } else {
                                if(!value.trim().equals("")){
                                    article.addAttribute(new Attribute(headers[g].trim(), value));
                                }
                            }
                        }
                        articles.add(article);
                    }
                    return articles;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        /**
         * Parsing any image file into article (except gif)
         * @param uri Location of asset on disk
         * @return Article representation of asset
         */
        public static Article image(String uri){

            return null;
        }

        /**
         * Parsing video to article
         * @param uri Location of asset on disk
         * @return Article representation of asset
         */
        public static Article video(String uri){

            return null;
        }

        /**
         * Parsing gif files into article
         * Requires separate parser engine due to file format
         * @param uri Location of asset on disk
         * @return Article representation of asset
         */
        public static Article gif(String uri){

            return null;
        }

        /**
         * Parsing textual files into articles
         * For example, docx, txt, rtf
         * @param uri Location of asset on disk
         * @return Article representation of asset
         */
        public static Article document(String uri){

            return null;
        }

        /**
         * Parsing pdf files into articles
         * @return Article representation of asset
         */
        public static Article pdf(){

            return null;
        }

        /**
         * Parse plain text into an article
         * @param text Plain text as string to parse
         * @return Article representing text
         */
        public static Article text(String text){

            return null;
        }
    }

    public static class connectors {

        public static ArrayList<Article> yelp(String[] zips, Configuration config) throws UnsupportedEncodingException {
            ArticlesClient articlesClient = new ArticlesClient(config);
            ArrayList<Article> articles = new ArrayList<>();
            String batchId = Telifie.tools.make.shortEid();
            String API_KEY = "IyhStCFRvjRbjE51NyND1w4JyKIiZ-3r4Qf1g-DquKCgi8bNJcqK0-EjNoxCen1y0H57JJxmteYzpj8uZ78LLAlMn3Ea0S8bjioBm_5CMYK-TnHwzQ0jC0UN-0tHZHYx";
            for(String zip : zips){
                String url = "https://api.yelp.com/v3/businesses/search?sort_by=best_match&limit=50&location=" + URLEncoder.encode(zip, StandardCharsets.UTF_8);
                HttpGet httpGet = new HttpGet(url);
                httpGet.addHeader("Authorization", "Bearer " + API_KEY);
                try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(httpResponse.getEntity().getContent());
                    JsonNode businessesNode = rootNode.path("businesses");
                    for (JsonNode businessNode : businessesNode) {
                        Article article = new Article();
                        article.setPriority(0.78);
                        article.setId(Telifie.tools.make.md5(businessNode.path("id").asText()));
                        if(!articlesClient.exists(article.getId())){
                            article.setIcon(businessNode.path("image_url").asText());
                            article.addAttribute(new Attribute("*batch", batchId));
                            article.addAttribute(new Attribute("Phone", businessNode.path("display_phone").asText()));
                            article.addAttribute(new Attribute("Rating", businessNode.path("rating").asText()));
                            article.addAttribute(new Attribute("Price", businessNode.path("price").asText()));
                            article.addAttribute(new Attribute("Reviews", businessNode.path("review_count").asText()));
                            article.addAttribute(new Attribute("Latitude", businessNode.path("coordinates").path("latitude").asText()));
                            article.addAttribute(new Attribute("Longitude", businessNode.path("coordinates").path("longitude").asText()));
                            String name = Telifie.tools.strings.htmlEscape(businessNode.path("name").asText());
                            article.setTitle(name);
                            String street = businessNode.path("location").path("address1").asText();
                            String city = businessNode.path("location").path("city").asText();
                            article.addAttribute(new Attribute("City", city));
                            article.addTag(city);
                            String state = businessNode.path("location").path("state").asText();
                            article.addAttribute(new Attribute("State", state));
                            String zipCode = businessNode.path("location").path("zip_code").asText();
                            article.addAttribute(new Attribute("Zip Code", zipCode));
                            article.addTag(zipCode);
                            article.addAttribute(new Attribute("Address", street + ", " + city + ", " + state + " " + zipCode));
                            article.setContent(name + " is located at " + street + ", " + city + ", " + state + " " + zipCode + ".");
                            businessNode.path("categories").forEach(category -> {
                                article.addTag(Telifie.tools.strings.htmlEscape(category.path("title").asText()));
                                article.setDescription(Telifie.tools.strings.sentenceCase(category.path("title").asText()));
                            });
                            article.setSource(new Source(
                                    "6a9aaefa90c5edc50d678cca8c78e520",
                                    "https://telifie-static.nyc3.cdn.digitaloceanspaces.com/wwdb-index-storage/yelp.png",
                                    "Yelp",
                                    businessNode.path("url").asText().split("\\?")[0]
                            ));
                            String businessId = businessNode.path("id").asText();
                            String photosUrl = "https://api.yelp.com/v3/businesses/" + businessId;
                            HttpGet photosHttpGet = new HttpGet(photosUrl);
                            photosHttpGet.addHeader("Authorization", "Bearer " + API_KEY);
                            try (CloseableHttpClient photosHttpClient = HttpClients.createDefault();
                                 CloseableHttpResponse photosHttpResponse = photosHttpClient.execute(photosHttpGet)) {
                                ObjectMapper photosObjectMapper = new ObjectMapper();
                                JsonNode photosRootNode = photosObjectMapper.readTree(photosHttpResponse.getEntity().getContent());
                                JsonNode photosNode = photosRootNode.path("photos");
                                for (JsonNode photoNode : photosNode) {
                                    String photoUrl = photoNode.asText();
                                    article.addImage(new Image(photoUrl, "", businessNode.path("url").asText()));
                                }
                            } catch (ClientProtocolException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            articlesClient.create(article);
                            articles.add(article);
                        }
                    }
                } catch (ClientProtocolException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return articles;
        }

        public static ArrayList<Article> spotify(){
            return null;
        }

        public static ArrayList<Article> tmdb(int page, int limit){
            ArrayList<Article> movies = new ArrayList<>();
            String apiKey = "991191cec151e1797c192c74f06b40a7";
            int pageNumber = page;
            while(pageNumber < (page + limit)){
                try {
                    String batchId = Telifie.tools.make.shortEid();
                    String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + apiKey + "&page=" + pageNumber;
                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                    System.out.println(con.getResponseCode());
                    System.out.println(con.getResponseMessage());
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    JSONObject myresponse = new JSONObject(response.toString());
                    JSONArray moviesArray = myresponse.getJSONArray("results");
                    for (int i = 0; i < moviesArray.length(); i++) {
                        Article article = new Article();
                        JSONObject movie = moviesArray.getJSONObject(i);
                        String movie_title = Telifie.tools.strings.htmlEscape(movie.getString("title"));
                        String movie_id = String.valueOf(movie.getInt("id"));
                        article.setId(Telifie.tools.make.md5(movie_id));
                        article.addAttribute(new Attribute("*batch", batchId));
                        article.setPriority(0.56);
                        article.setTitle(movie_title);
                        article.setDescription("Movie");
                        article.setContent(Telifie.tools.strings.htmlEscape(movie.getString("overview")));
                        if(!movie.isNull("poster_path")){
                            article.setIcon("https://image.tmdb.org/t/p/original" + movie.getString("poster_path"));
                            article.addImage(
                                    new Image(
                                            "https://image.tmdb.org/t/p/original" + movie.getString("poster_path"),
                                            movie_title + " Poster",
                                            "https://www.themoviedb.org/movie/" + movie_id));
                        }
                        if(!movie.isNull("backdrop_path")){
                            article.addImage(
                                    new Image(
                                            "https://image.tmdb.org/t/p/original" + movie.getString("backdrop_path"),
                                            movie_title + " Backdrop",
                                            "https://www.themoviedb.org/movie/" + movie_id));
                        }
                        if(!movie.isNull("release_date") && !movie.getString("release_date").equals("")){
                            article.addAttribute(new Attribute("Released", convertDateFormat(movie.getString("release_date")))); //TODO format release date
                        }
                        article.setSource(new Source(
                                "1adc674b-12ce-4428-8874-4a4445c13617",
                                "https://telifie-static.nyc3.digitaloceanspaces.com/mirror/uploads/articles/icons/37430239-78ae-4bf7-8245-8d8969f99011_apple-touch-icon-57ed4b3b0450fd5e9a0c20f34e814b82adaa1085c79bdde2f00ca8787b63d2c4.png",
                                "The Movie Database",
                                "https://www.themoviedb.org/movie/" + movie_id
                        ));
                        String url2 = "https://api.themoviedb.org/3/movie/" + movie_id + "?api_key=" + apiKey;
                        URL obj2 = new URL(url2);
                        System.out.println(article);
                        if(!articles.exists(article.getId())){
                            try{
                                HttpURLConnection con2 = (HttpURLConnection) obj2.openConnection();
                                BufferedReader in2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));
                                String inputLine2;
                                StringBuffer response2 = new StringBuffer();
                                while ((inputLine2 = in2.readLine()) != null) {
                                    response2.append(inputLine2);
                                }
                                in2.close();
                                JSONObject myresponse2 = new JSONObject(response2.toString());
                                article.setLink(myresponse2.getString("homepage"));
                                try {
                                    int rev = myresponse2.getInt("revenue");
                                    int bud = myresponse2.getInt("budget");
                                    int runtime = myresponse2.getInt("runtime");
                                    NumberFormat formatter = NumberFormat.getInstance();
                                    String fRevenue = formatter.format(rev);
                                    String fBudget = formatter.format(bud);

                                    if(rev > 0){
                                        article.addAttribute(new Attribute("Revenue", "$" + fRevenue));
                                    }
                                    if(bud > 0){
                                        article.addAttribute(new Attribute("Budget", "$" + fBudget));
                                    }
                                    if(runtime > 0){
                                        article.addAttribute(new Attribute("Runtime", simplifyRuntime(runtime)));
                                    }

                                    try {
                                        String url3 = "https://api.themoviedb.org/3/movie/" + movie_id + "/credits?api_key=" + apiKey;
                                        URL obj3 = new URL(url3);
                                        HttpURLConnection con3 = (HttpURLConnection) obj3.openConnection();
                                        BufferedReader in3 = new BufferedReader(new InputStreamReader(con3.getInputStream()));
                                        String inputLine3;
                                        StringBuffer response3 = new StringBuffer();
                                        while ((inputLine3 = in3.readLine()) != null) {
                                            response3.append(inputLine3);
                                        }
                                        in3.close();
                                        JSONObject myresponse3 = new JSONObject(response3.toString());
                                        JSONArray cast = myresponse3.getJSONArray("cast");
                                        Association ass = new Association("https://telifie-static.nyc3.cdn.digitaloceanspaces.com/images/associations/cast.png", "Cast");
                                        for(int d = 0; d < cast.length(); d++){
                                            JSONObject c = cast.getJSONObject(d);
                                            String position = "Casting", profile = "https://telifie-static.nyc3.cdn.digitaloceanspaces.com/images/casting.png", job = c.getString("known_for_department");
                                            if(!c.isNull("profile_path")){
                                                profile = "https://image.tmdb.org/t/p/original" + c.getString("profile_path");
                                            }
                                            if(!c.isNull("job")){
                                                job = c.getString("job");
                                            }
                                            if(!c.isNull("character")){
                                                position = Telifie.tools.strings.htmlEscape(c.getString("character"));
                                            }else if(!c.isNull("department")) {
                                                position = c.getString("department");
                                            }
                                            String name = Telifie.tools.strings.htmlEscape(c.getString("name"));
                                            ass.addArticle(new Child(profile, name, position));
                                        }
                                        article.addAssociation(ass);
                                    } catch (Exception e) {
                                        System.out.println("Could not parse credits");
                                    }
                                } catch (JSONException e) {
                                    System.out.println("Could not parse number from JSON");
                                }
                            }catch(FileNotFoundException e){
                                System.out.println("\n\nNo Detail query");
                            }
                            articles.create(article);
                        }else{
                            System.out.println("Exists");
                        }
                        movies.add(article);
                    }
                    pageNumber++;
                    System.out.println("Onto page " + pageNumber);
                    Thread.sleep(8000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return movies;
        }

        public static ArrayList<Article> tmdbShows(int page, int limit){
            ArrayList<Article> movies = new ArrayList<>();
            String apiKey = "991191cec151e1797c192c74f06b40a7";
            int pageNumber = page;
            while(pageNumber < (page + limit)){
                try {
                    String batchId = Telifie.tools.make.shortEid();
                    String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + apiKey + "&page=" + pageNumber;
                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                    System.out.println(con.getResponseCode());
                    System.out.println(con.getResponseMessage());
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    JSONObject myresponse = new JSONObject(response.toString());
                    JSONArray moviesArray = myresponse.getJSONArray("results");
                    for (int i = 0; i < moviesArray.length(); i++) {
                        Article article = new Article();
                        JSONObject movie = moviesArray.getJSONObject(i);
                        String movie_title = Telifie.tools.strings.htmlEscape(movie.getString("title"));
                        String movie_id = String.valueOf(movie.getInt("id"));
                        article.setId(Telifie.tools.make.md5(movie_id));
                        article.addAttribute(new Attribute("*batch", batchId));
                        article.setPriority(0.56);
                        article.setTitle(movie_title);
                        article.setDescription("Movie");
                        article.setContent(Telifie.tools.strings.htmlEscape(movie.getString("overview")));
                        if(!movie.isNull("poster_path")){
                            article.setIcon("https://image.tmdb.org/t/p/original" + movie.getString("poster_path"));
                            article.addImage(
                                    new Image(
                                            "https://image.tmdb.org/t/p/original" + movie.getString("poster_path"),
                                            movie_title + " Poster",
                                            "https://www.themoviedb.org/movie/" + movie_id));
                        }
                        if(!movie.isNull("backdrop_path")){
                            article.addImage(
                                    new Image(
                                            "https://image.tmdb.org/t/p/original" + movie.getString("backdrop_path"),
                                            movie_title + " Backdrop",
                                            "https://www.themoviedb.org/movie/" + movie_id));
                        }
                        if(!movie.isNull("release_date") && !movie.getString("release_date").equals("")){
                            article.addAttribute(new Attribute("Released", convertDateFormat(movie.getString("release_date")))); //TODO format release date
                        }
                        article.setSource(new Source(
                                "1adc674b-12ce-4428-8874-4a4445c13617",
                                "https://telifie-static.nyc3.digitaloceanspaces.com/mirror/uploads/articles/icons/37430239-78ae-4bf7-8245-8d8969f99011_apple-touch-icon-57ed4b3b0450fd5e9a0c20f34e814b82adaa1085c79bdde2f00ca8787b63d2c4.png",
                                "The Movie Database",
                                "https://www.themoviedb.org/movie/" + movie_id
                        ));
                        String url2 = "https://api.themoviedb.org/3/movie/" + movie_id + "?api_key=" + apiKey;
                        URL obj2 = new URL(url2);
                        System.out.println(article);
                        if(!articles.exists(article.getId())){
                            try{
                                HttpURLConnection con2 = (HttpURLConnection) obj2.openConnection();
                                BufferedReader in2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));
                                String inputLine2;
                                StringBuffer response2 = new StringBuffer();
                                while ((inputLine2 = in2.readLine()) != null) {
                                    response2.append(inputLine2);
                                }
                                in2.close();
                                JSONObject myresponse2 = new JSONObject(response2.toString());
                                article.setLink(myresponse2.getString("homepage"));
                                try {
                                    int rev = myresponse2.getInt("revenue");
                                    int bud = myresponse2.getInt("budget");
                                    int runtime = myresponse2.getInt("runtime");
                                    NumberFormat formatter = NumberFormat.getInstance();
                                    String fRevenue = formatter.format(rev);
                                    String fBudget = formatter.format(bud);

                                    if(rev > 0){
                                        article.addAttribute(new Attribute("Revenue", "$" + fRevenue));
                                    }
                                    if(bud > 0){
                                        article.addAttribute(new Attribute("Budget", "$" + fBudget));
                                    }
                                    if(runtime > 0){
                                        article.addAttribute(new Attribute("Runtime", simplifyRuntime(runtime)));
                                    }

                                    try {
                                        String url3 = "https://api.themoviedb.org/3/movie/" + movie_id + "/credits?api_key=" + apiKey;
                                        URL obj3 = new URL(url3);
                                        HttpURLConnection con3 = (HttpURLConnection) obj3.openConnection();
                                        BufferedReader in3 = new BufferedReader(new InputStreamReader(con3.getInputStream()));
                                        String inputLine3;
                                        StringBuffer response3 = new StringBuffer();
                                        while ((inputLine3 = in3.readLine()) != null) {
                                            response3.append(inputLine3);
                                        }
                                        in3.close();
                                        JSONObject myresponse3 = new JSONObject(response3.toString());
                                        JSONArray cast = myresponse3.getJSONArray("cast");
                                        Association ass = new Association("https://telifie-static.nyc3.cdn.digitaloceanspaces.com/images/associations/cast.png", "Cast");
                                        for(int d = 0; d < cast.length(); d++){
                                            JSONObject c = cast.getJSONObject(d);
                                            String position = "Casting", profile = "https://telifie-static.nyc3.cdn.digitaloceanspaces.com/images/casting.png", job = c.getString("known_for_department");
                                            if(!c.isNull("profile_path")){
                                                profile = "https://image.tmdb.org/t/p/original" + c.getString("profile_path");
                                            }
                                            if(!c.isNull("job")){
                                                job = c.getString("job");
                                            }
                                            if(!c.isNull("character")){
                                                position = Telifie.tools.strings.htmlEscape(c.getString("character"));
                                            }else if(!c.isNull("department")) {
                                                position = c.getString("department");
                                            }
                                            String name = Telifie.tools.strings.htmlEscape(c.getString("name"));
                                            ass.addArticle(new Child(profile, name, position));
                                        }
                                        article.addAssociation(ass);
                                    } catch (Exception e) {
                                        System.out.println("Could not parse credits");
                                    }
                                } catch (JSONException e) {
                                    System.out.println("Could not parse number from JSON");
                                }
                            }catch(FileNotFoundException e){
                                System.out.println("\n\nNo Detail query");
                            }
                            articles.create(article);
                        }else{
                            System.out.println("Exists");
                        }
                        movies.add(article);
                    }
                    pageNumber++;
                    System.out.println("Onto page " + pageNumber);
                    Thread.sleep(8000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return movies;
        }

        public static String convertDateFormat(String inputDate) {
            try {
                DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate date = LocalDate.parse(inputDate, inputFormat);

                DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                return date.format(outputFormat);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Expected format is 'yyyy-MM-dd'");
                return null;
            }
        }

        public static String simplifyRuntime(int minutes){
            if(minutes < 60){
                return minutes + " Minutes";
            } else {
                int hours = minutes / 60;
                int remainingMinutes = minutes % 60;
                return hours + (hours > 1 ? " Hours " : " Hour ") + remainingMinutes + " Minutes";
            }
        }

    }

    public class webpage {
        public static Article extract(String url, Document document){
            Article article = new Article();
            article.setDescription("Webpage");
            Elements metaTags = document.getElementsByTag("meta");
            for (Element tag : metaTags){
                String mtn = tag.attr("name");
                String mtc = Telifie.tools.strings.htmlEscape(tag.attr("content"));
                switch (mtn) {
                    case "description" -> {
                        if (!tag.attr("content").trim().equals("")) {
                            article.setContent(mtc);
                        }
                    }
                    case "keywords" -> {
                        String[] words = mtc.split(",");
                        for (String word : words) {
                            article.addTag(word.trim().toLowerCase());
                        }
                    }
                    case "og:image" -> article.addImage(new Image(mtc, "", url));
                }
            }
            Elements linkTags = document.getElementsByTag("link");
            for (Element linkTag : linkTags){
                String rel = linkTag.attr("rel");
                String href = linkTag.attr("href");
                if(rel.contains("icon")){
                    try {
                        article.setIcon(Telifie.tools.detector.fixLink("https://" + new URL(url).getHost(), href));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            Elements images = document.getElementsByTag("img");
            for(Element image : images){
                String src = Telifie.tools.detector.fixLink(url, image.attr("src"));
                String srcset = Telifie.tools.detector.fixLink(url, image.attr("srcset"));
                if(!src.equals("") && !src.equals("null") && Telifie.tools.detector.getType(src).equals("image") && !image.attr("src").trim().toLowerCase().startsWith("data:")){
                    String caption = Telifie.tools.strings.htmlEscape(image.attr("alt").replaceAll("“", "").replaceAll("\"", "&quote;").trim());
                    if(!caption.equals("Page semi-protected") && !caption.equals("Wikimedia Foundation") && !caption.equals("Powered by MediaWiki") && !caption.equals("Edit this at Wikidata") && !caption.equals("This is a good article. Click here for more information.")){
                        Image img = new Image(src, caption, url);
                        article.addImage(img);
                    }
                }else if(!srcset.equals("") && !srcset.startsWith("data:")){
                    String caption =  Telifie.tools.strings.htmlEscape(image.attr("alt").replaceAll("“", "").replaceAll("\"", "&quote;"));
                    Image img = new Image(src, caption, url);
                    article.addImage(img);
                }
            }
            article.setTitle(Telifie.tools.strings.htmlEscape(document.title()));
            article.setLink(url);
            String whole_text = document.text().replaceAll("[\n\r]", " ");
            if(article.getContent() == null || article.getContent().equals("")){
                Element body = document.getElementsByTag("body").get(0);
                body.select("table, script, header, style, img, svg, button, label, form, input, aside, code, nav").remove();
                if(url.contains("wiki")){
                    article.setSource(
                            new Source(
                                    "bb9ae95c2b59f2c7d8a81ded769d3bab",
                                    "https://telifie-static.nyc3.digitaloceanspaces.com/wwdb-index-storage/wikipedia.png",
                                    "Wikipedia",
                                    article.getLink().trim()
                            )
                    );
                    article.setLink(null);
                    article.setTitle(article.getTitle().replaceAll(" - Wikipedia", ""));
                    body.select("div.mw-jump-link, div#toc, div.navbox, table.infobox, div.vector-body-before-content, div.navigation-not-searchable, div.mw-footer-container, div.reflist, div#See_also, h2#See_also, h2#References, h2#External_links").remove();
                }else{
                    Matcher phone_numbers = Telifie.tools.detector.findPhoneNumbers(whole_text);
                    while(phone_numbers.find()){
                        String phone_number = phone_numbers.group().trim().replaceAll("[^0-9]", "").replaceFirst("(\\d{3})(\\d{3})(\\d+)", "($1) $2 – $3");
                        Attribute attr = new Attribute("Phone", phone_number);
                        article.addAttribute(attr);
                    }
                }
                StringBuilder markdown = new StringBuilder();
                Elements paragraphs = body.select("p, h3");
                for (Element element : paragraphs) {
                    if (element.tagName().equalsIgnoreCase("p")) {
                        String text = StringEscapeUtils.escapeHtml4(element.text().replaceAll("\\s+", " ").trim());
                        if(!text.equals("")){
                            markdown.append("  \n").append(text).append("  \n");
                        }
                    } else if (element.tagName().equalsIgnoreCase("h3")) {
                        String headerText = Telifie.tools.strings.escape(element.text().trim());
                        markdown.append("##### ").append(headerText).append("  \n");
                    }
                }
                String md = StringEscapeUtils.escapeJson(markdown.toString().replaceAll("\\[.*?]", "").trim());
                article.setContent(md);
            }
            return article;
        }
    }

    public static class RobotPermission {
        private final List<String> disallowed = new ArrayList<>();
        public RobotPermission(String host) {
            try {
                URL url = new URL(host + "/robots.txt");
                try (Scanner scanner = new Scanner(url.openStream())) {
                    boolean isUserAgent = false;
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim();
                        if (line.toLowerCase().startsWith("user-agent")) {
                            isUserAgent = line.substring(11).trim().equals("*") || line.substring(11).trim().equals("telifie/1.0");
                        } else if (isUserAgent) {
                            if (line.toLowerCase().startsWith("disallow")) {
                                disallowed.add(line.substring(9).trim());
                            } else if (line.toLowerCase().startsWith("user-agent")) {
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public boolean isAllowed(String path) {
            return disallowed.stream().noneMatch(path::startsWith);
        }
    }

    public ArrayList<Article> getTraversable() {
        return traversable;
    }

    public void purge(){
        traversable.clear();
    }

    public static boolean isParsed(String uri){
        String alt = uri;
        if(uri.endsWith("/")){
            alt = uri.substring(0, uri.length() - 1);
        }
        for (String s : Parser.parsed) {
            if (s.equals(alt) || s.equals(uri)) {
                return true;
            }
        }
        return false;
    }
}