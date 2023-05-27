package com.telifie.Models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.telifie.Models.Articles.*;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Utilities.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Parser {

    private static String uri, host;
    private static final ArrayList<Article> traversable = new ArrayList<>();
    private static final ArrayList<String> parsed = new ArrayList<>();
    private static int MAX_DEPTH = 1;

    public static class engines {

        public static Article parse(String uri){
            traversable.removeAll(traversable);
            Parser.uri = uri;
            Telifie.console.out.string("Parser URI Attempt on -> " + uri);
            if(Telifie.tools.detector.isUrl(uri)){ //Crawl website if url
                try {
                    host = new URL(uri).getHost();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                Article crawled = Parser.engines.website(uri, 0);
                Telifie.console.out.string(traversable.toString());
                return crawled;
            }else if(Telifie.tools.detector.isFile(uri)){ //Parsing a file
                File file = new File(uri);
                if(file.exists()){

                }
            }
            return null;
        }

        public static Article website(String url, int depth){

            if(depth > MAX_DEPTH || !url.contains(host)){
                return null;
            }
            parsed.add(url);
            try {
                Connection.Response response = Jsoup.connect(url).userAgent("telifie/1.0").execute();
                if(response.statusCode() == 200){
                    Document root = response.parse();
                    Article article = Webpage.extract(url, root);

                    ArrayList<String> links = Telifie.tools.make.extractLinks(root.getElementsByTag("a"), uri);
                    if(links.size() > 0){

                        Association pages = new Association("https://telifie-static.nyc3.cdn.digitaloceanspaces.com/images/associations/pages.png", "Pages");
                        for(String link : links){

                            boolean isNotParsed = !isParsed(link);
                            //Link can't have been parsed and must be parsable as different website
                            if(isNotParsed) {
                                Article child = website(link, depth + 1);
                                if (child != null) { //Then should be making an association
                                    String referenceString = child.getTitle();
                                    Child child_association = new Child(child.getId(), child.getIcon(), referenceString, "Page");
                                    child_association.setId(child.getId());
                                    pages.addArticle(child_association);
                                }
                            }else{
                                if(Telifie.tools.strings.containsAnyOf(new String[]{"facebook", "instagram", "spotify", "linkedin", "youtube"}, link)){

                                    String value = link;
                                    String[] parts = host.split("\\.");
                                    String domain = parts[parts.length - 2];
                                    String capitalizedDomain = domain.substring(0, 1).toUpperCase() + domain.substring(1);
                                    if(Telifie.tools.detector.isUrl(value)){
                                        value = "@" + value;
                                    }
                                    article.addAttribute(new Attribute(capitalizedDomain, value));
                                }
                            } //Not link that belongs to parent
                        } //End for loop
                        if(pages.size() >= 1){
                            article.addAssociation(pages);
                        }
                    } //End if links > 0
                    if(article.getLink().contains(new URL(uri).getHost())) {
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
                    int titleIndex = 0, descriptionIndex = 0, iconIndex = 0, linkIndex = 0, contentIndex = 0;
                    for (int i = 0; i < headers.length; i++) {
                        String hV = lines.get(0)[i].toLowerCase().trim();
                        switch (hV) {
                            case "title" -> titleIndex = i;
                            case "description" -> descriptionIndex = i;
                            case "link" -> linkIndex = i;
                            case "content" -> contentIndex = i;
                            case "icon" -> iconIndex = i;
                        }
                    }
                    String batchId = Telifie.tools.make.shortEid().toLowerCase();
                    int total = lines.size() - 1;
                    for (int i = 1; i < lines.size() - 1; i++) {
                        Telifie.console.out.string("Parsing article " + i + " of " + total);
                        String[] articleData = lines.get(i);
                        Article article = new Article();
                        article.setPriority(withPriority);
                        article.addAttribute(new Attribute("*batch", batchId));
                        for (int g = 0; g < articleData.length; g++) {
                            String value = articleData[g];
                            if (g == titleIndex) {
                                article.setTitle(value);
                            } else if (g == descriptionIndex) {
                                article.setDescription(value);
                            } else if (g == linkIndex) {
                                article.setLink(value);
                            } else if (g == contentIndex) {
                                article.setContent(value);
                            } else if(g == iconIndex){
                                article.setIcon(value);
                            } else { //Not specified value

                                //TODO Do special stuff with attributes
                                if(!value.trim().equals("")){
                                    article.addAttribute(new Attribute(headers[g], value));
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

    /**
     * Parser.connectors is for parsing content from connectors
     */
    public static class connectors {

        public static ArrayList<Article> yelp(String[] zips, Configuration config) throws UnsupportedEncodingException {
            ArticlesClient articlesClient = new ArticlesClient(config);
            ArrayList<Article> articles = new ArrayList<>();
            String batchId = Telifie.tools.make.shortEid();
            String API_KEY = "IyhStCFRvjRbjE51NyND1w4JyKIiZ-3r4Qf1g-DquKCgi8bNJcqK0-EjNoxCen1y0H57JJxmteYzpj8uZ78LLAlMn3Ea0S8bjioBm_5CMYK-TnHwzQ0jC0UN-0tHZHYx";
            int total_count = 0, saved = 0;
            for(String zip : zips){
                String url = "https://api.yelp.com/v3/businesses/search?sort_by=best_match&limit=50&location=" + URLEncoder.encode(zip, StandardCharsets.UTF_8);
                HttpGet httpGet = new HttpGet(url);
                httpGet.addHeader("Authorization", "Bearer " + API_KEY);
                try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(httpResponse.getEntity().getContent());
                    JsonNode businessesNode = rootNode.path("businesses");
                    int i = 0;
                    total_count += businessesNode.size();
                    for (JsonNode businessNode : businessesNode) {
                        i++;
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
                                    businessNode.path("url").asText()
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
                            saved += 1;
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
            Telifie.console.out.line();
            Telifie.console.out.string("Total articles found: " + total_count);
            Telifie.console.out.string("Total articles saved: " + saved);
            Telifie.console.out.line();
            return articles;
        }
    }

    public ArrayList<Article> getTraversable() {
        return traversable;
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