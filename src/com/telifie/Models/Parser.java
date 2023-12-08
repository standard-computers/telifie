package com.telifie.Models;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Unit;
import com.telifie.Models.Clients.Sql;
import com.telifie.Models.Clients.TimelinesClient;
import com.telifie.Models.Utilities.Console;
import com.telifie.Models.Utilities.Event;
import com.telifie.Models.Articles.*;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Utilities.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.telifie.Models.Actions.Search;

public class Parser {

    private static ArticlesClient articles;

    public Parser(Session session){
        articles = new ArticlesClient(session);
    }

    public static boolean isParsed(String uri){
        return new Sql().isParsed(uri);
    }

    /**
     * Will start reparsing the domain
     */
    public static void reparse(){
        Log.message("STARTING REPARSE");
        ArticlesClient articles =  new ArticlesClient(new Session("com.telifie.master_data_team", "telifie"));
        TimelinesClient timelines = new TimelinesClient(new Session("com.telifie.master_data_team", "telifie"));
        ArrayList<Article> parsing = articles.withProjection(new org.bson.Document("link", Search.pattern("https://")), new org.bson.Document("link", 1));
        Console.log("RE-PARSE TOTAL : " + parsing.size());
        parsing.forEach(a -> {
            timelines.addEvent(a.getId(), Event.Type.CRAWL);
            engines.fetch(a.getLink(), 0, false);
        });
    }

    /**
     * Initial stage to parsing a URI
     * This method determines which parsing engine to use.
     * @param uri URI of asset location
     * @return Article of parsed asset
     */
    public static Article parse(String uri){
        if(Asset.isWebpage(uri)){
            return Parser.engines.website(uri);
        }else if(Asset.isFile(uri)){
            File file = new File(uri);
            if(file.exists()){
                return Parser.engines.website(uri);
            }
        }else{
            Asset asset = new Asset(uri);
            asset.download();
            if(asset.getExt().equals("md")){
                return Parser.engines.markdown(asset);
            }else if(asset.getExt().equals("txt")){
                return Parser.engines.text(asset);
            }else if(Asset.getType(uri).equals("image")){
                return Parser.engines.image(uri);
            }
        }
        return null;
    }

    public static class engines {

        /**
         * Entry point to fetch for crawling websites
         * @param uri URI of webpage being parsed
         * @param allowExternalCrawl Allow crawling of websites not part of URI host?
         * @return
         */
        public static Article crawler(String uri, boolean allowExternalCrawl){
            return Parser.engines.fetch(uri, 0, allowExternalCrawl);
        }

        private static Article fetch(String url, int depth, boolean allowExternalCrawl){
            depth++;
            if(isParsed(url)){
                return null;
            }
            try {
                URL urlObj = new URL(url);
                String host = urlObj.getProtocol() + "://" + urlObj.getHost();
                RobotPermission robotPermission = new RobotPermission(host);
                if(!robotPermission.isAllowed(urlObj.getPath())){
                    Log.error("ROBOTS DISALLOWED : " + url);
                    return null;
                }
                Connection.Response response = Jsoup.connect(url).userAgent("telifie/1.0").execute();
                if(response.statusCode() == 200){
                    Log.message("PARSING : " + url);
                    new Sql().parsed("com.telifie.master_data_team", url);
                    webpage wp = new webpage();
                    Article article = wp.extract(url, response.parse());
                    Article refArticle = articles.withLink(url);
                    if(refArticle != null || articles.lookup(article.getLink())){ //article exists with requested parse url
                        //TODO update article
                        if(refArticle.getSource() != null){//TODO this line is good don't change

                        }
                    }else{
                        articles.create(article);
                        Console.log("ARTICLE CREATED : " + url);
                    }
                    ArrayList<String> links = wp.getLinks();
                    int finalDepth = depth;
                    links.forEach(link -> {
                        String href = fixLink(host, link.split("\\?")[0]);
                        if(Asset.isWebpage(href) && !Andromeda.tools.contains(new String[]{"facebook.com", "instagram.com", "spotify.com", "linkedin.com", "youtube.com", "pinterest.com", "twitter.com", "tumblr.com", "reddit.com"}, href)){
                            if((allowExternalCrawl && !href.contains(host)) || (!allowExternalCrawl && href.contains(host))){
                                try {
                                    Thread.sleep(2000);
                                    if(finalDepth <= 2){
                                        fetch(href, finalDepth, allowExternalCrawl);
                                    }
                                } catch (InterruptedException e) {
                                    Log.error("FAILED TO SLEEP THREAD : PARSER");
                                }
                            }
                        }
                    });
                    return article;
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        }

        /**
         * Parsing a single webpage
         * @param url URL of the webpage being parsed
         * @return Article of webpage asset
         */
        public static Article website(String url){
            try {
                Connection.Response response = Jsoup.connect(url).userAgent("telifie/1.0").execute();
                Log.message("PARSING : " + response.statusCode() + " : " + url);
                if(response.statusCode() == 200){
                    Document root = response.parse(); //Convert HTML string to Document
                    return new webpage().extract(url, root); //Create basic article from extracting webpage
                }
                Log.error(response.statusCode() + " : " + url);
                return null;
            } catch (IOException e) {
                Log.error("FAILED CONNECTING TO HOST : " + url);
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
                    Files.copy(inputStream, Paths.get(Telifie.configDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1]), StandardCopyOption.REPLACE_EXISTING);
                    ArrayList<String[]> lines = new ArrayList<>();
                    try (CSVReader reader = new CSVReader(new FileReader(Telifie.configDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1]))) {
                        String[] fields;
                        while ((fields = reader.readNext()) != null) {
                            lines.add(fields);
                        }
                    } catch (IOException | CsvException e) {
                        Log.error("FAILED CSV FILE READ : PARSER / BATCH");
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
                    String batchId = String.valueOf(Telifie.epochTime());
                    for (int i = 1; i < lines.size(); i++) {
                        String[] articleData = lines.get(i);
                        Article article = new Article();
                        article.setPriority(withPriority);
                        article.addAttribute(new Attribute("*batch", batchId));
                        for (int g = 0; g < articleData.length; g++) {
                            String value = articleData[g];
                            if (g == titleIndex && titleIndex > -1) {
                                article.setTitle(Andromeda.tools.escape(value));
                            } else if (g == descriptionIndex && descriptionIndex > -1) {
                                article.setDescription(value);
                            } else if (g == linkIndex && linkIndex > -1) {
                                article.setLink(value);
                            } else if (g == contentIndex && contentIndex > -1) {
                                article.setContent(Andromeda.tools.escape(value));
                            } else if(g == iconIndex && iconIndex > -1){
                                article.setIcon(value);
                            } else if(g == tagsIndex){
                                String[] tags = value.split(",");
                                for(String tag : tags){
                                    article.addTag(tag.toLowerCase().trim());
                                }
                            } else {
                                if(!value.trim().isEmpty()){
                                    article.addAttribute(new Attribute(headers[g].trim(), value));
                                }
                            }
                        }
                        articles.add(article);
                    }
                    return articles;
                } catch (IOException e) {
                    Log.error("FAILED BATCH UPLOAD");
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
            Article a = new Article();
            a.setIcon(uri);
            a.setLink(uri);
            a.setDescription("Image");
            return a;
        }

        /**
         * Parsing video to article
         * @param uri Location of asset on disk
         * @return Article representation of asset
         */
        public static Article video(String uri){
            Article a = new Article();
            a.setDescription("Video");
            return null;
        }

        /**
         * Parsing docx like files into articles
         * @param uri Location of asset on disk
         * @return Article representation of asset
         */
        public static Article document(String uri){
            Article a = new Article();
            a.setDescription("Document");
            return null;
        }

        /**
         * Parsing pdf files into articles
         * @return Article representation of asset
         */
        public static Article pdf(){
            Article a = new Article();
            a.setDescription("Document");
            return null;
        }

        /**
         * Parsing markdown file into Article
         * @param asset of Asset type. See Asset
         * @return Article of asset
         */
        public static Article markdown(Asset asset){
            Article a = new Article();
            Unit u = new Unit(asset.getContents());
            String[] keywords = u.keywords(6);
            a.setTitle(asset.getUri().split("/")[asset.getUri().split("/").length - 1].split("\\.")[0]);
            a.setDescription("Document");
            a.addTags(keywords);
            a.setContent(Andromeda.tools.escapeMarkdownForJson(Andromeda.tools.htmlEscape(asset.getContents())));
            a.addAttribute(new Attribute("File Type", "Markdown"));
            a.addAttribute(new Attribute("Size", asset.fileSize()));
            a.addAttribute(new Attribute("Lines", asset.lineCount()));
            a.addAttribute(new Attribute("Words", asset.wordCount()));
            return a;
        }

        /**
         * Parsing plain text files into Article
         * @param asset Asset of text file. See Asset.
         * @return Article of text file asset.
         */
        public static Article text(Asset asset){
            Article a = new Article();
            Unit u = new Unit(asset.getContents());
            String[] keywords = u.keywords(6);
            a.setTitle(asset.getUri().split("/")[asset.getUri().split("/").length - 1].split("\\.")[0]);
            a.setDescription("Document");
            a.addTags(keywords);
            a.setContent(Andromeda.tools.escapeMarkdownForJson(Andromeda.tools.htmlEscape(asset.getContents())));
            a.addAttribute(new Attribute("File Type", "Plain Text"));
            a.addAttribute(new Attribute("Size", asset.fileSize()));
            a.addAttribute(new Attribute("Lines", asset.lineCount()));
            a.addAttribute(new Attribute("Words", asset.wordCount()));
            return a;
        }
    }

    public static class webpage {

        private ArrayList<String> links;
        private String root;

        public Article extract(String url, Document document){
            Article article = new Article();
            try {
                URL rootUri = new URL(url);
                root = rootUri.getProtocol() + "://" + rootUri.getHost() + "/";
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            article.setTitle(Andromeda.tools.htmlEscape(document.title()));
            article.setLink(url);
            document.getElementsByTag("meta").forEach(tag -> {
                String mtn = tag.attr("name");
                String mtc = Andromeda.tools.htmlEscape(tag.attr("content"));
                switch (mtn) {
                    case "keywords" -> {
                        String[] words = mtc.split(",");
                        for (String word : words) {
                            article.addTag(word.trim().toLowerCase());
                        }
                    }
                }
            });

            links = Parser.extractLinks(document.getElementsByTag("a"), root); //Get urls
            if(!links.isEmpty()){ //For parsing each link for attributes
                links.forEach(link -> {
                    if(Andromeda.tools.contains(new String[]{"dribbble.com", "facebook.com", "instagram.com", "spotify.com", "linkedin.com", "youtube.com", "pinterest.com", "github.com", "twitter.com", "tumblr.com", "reddit.com"}, link)){
                        URI uri;
                        try {
                            uri = new URI(link);
                            String domain = uri.getHost();
                            String k = (domain.split("\\.")[0].equals("www") ? domain.split("\\.")[1] : domain.split("\\.")[0]);
                            k = k.substring(0, 1).toUpperCase() + k.substring(1);
                            String[] l = link.split("\\?")[0].split("/");
                            String un = l[l.length - 1].trim();
                            article.addAttribute(new Attribute(k, un));
                        } catch (URISyntaxException e) {
                            Log.error("CANNOT CONVERT URI TO URL : " + link);
                        }
                    }
                });
            }

            //Going through link tags for website icon
            document.getElementsByTag("link").forEach(linkTag -> {
                String rel = linkTag.attr("rel");
                String href = linkTag.attr("href");
                if(rel.contains("icon") && !url.contains("/wiki")){
                    try {
                        article.setIcon(fixLink("https://" + new URL(url).getHost(), href));
                    } catch (MalformedURLException e) {
                        Log.error("CANNOT CONVERT URI TO URL : " + url);
                    }
                }
            });

            document.getElementsByTag("img").forEach(image -> {
                String src = fixLink(url, image.attr("src"));
                if(!src.isEmpty() && !src.startsWith("data:")){
                    CompletableFuture.runAsync(() -> {
                        Asset ass = new Asset(src);
                        int[] d = ass.getDimensions();
                        if (d[0] > 42 && d[1] > 42) {
                            if (url.contains("/wiki")) {
                                if (article.getIcon() == null || article.getIcon().isEmpty()) {
                                    article.setIcon(src);
                                }
                            }
                            String caption = Andromeda.tools.htmlEscape(image.attr("alt").replaceAll("“", "").replaceAll("\"", "&quote;"));
                            Article ia = new Article();
                            if (caption != null && !caption.isEmpty() && caption.length() < 100) {
                                ia.setTitle(caption);
                            } else {
                                ia.setTitle(article.getTitle());
                                ia.setContent(caption);
                            }
                            ia.setLink(src);
                            ia.setIcon(src);
                            ia.setDescription("Image");
                            ia.addAttribute(new Attribute("Width", d[0] + "px"));
                            ia.addAttribute(new Attribute("Height", d[1] + "px"));
                            ia.addAttribute(new Attribute("Size", ass.fileSize()));
                            ia.addAttribute(new Attribute("File Type", ass.getExt()));
                            Article source = articles.withLink(root);
                            if (source != null) {
                                ia.setSource(new Source(source.getIcon(), source.getTitle(), url));
                            } else {
                                ia.setSource(new Source(article.getIcon(), article.getTitle(), url));
                            }
                            if(articles.create(ia)){
                                Console.log("IMAGE CREATED : " + src);
                            }
                        }
                    });
                }
            });

            //Preparing page content for Article content and other data extraction
            Element body = document.getElementsByTag("body").get(0);
            body.select("table, script, header, style, img, svg, button, label, form, input, aside, code, footer, nav").remove();
            if(url.contains("/wiki")){ //Wiki specific parsing
                article.setSource(new Source("https://telifie-static.nyc3.digitaloceanspaces.com/wwdb-index-storage/wikipedia.png", "Wikipedia", article.getLink().trim()));
                article.setLink(null);
                article.setTitle(article.getTitle().replaceAll(" - Wikipedia", ""));
                body.select("div.mw-jump-link, div#toc, div.navbox, table.infobox, div.vector-body-before-content, div.navigation-not-searchable, div.mw-footer-container, div.reflist, div#See_also, h2#See_also, h2#References, h2#External_links").remove();
            }else{

                String whole_text = document.text().replaceAll("[\n\r]", " "); //Extract Prices
                Pattern pattern = Pattern.compile("\\$\\d+(\\.\\d{2})?");
                Matcher matcher = pattern.matcher(whole_text);
                if (matcher.find()) {
                    String priceValue = matcher.group();
                    article.addAttribute(new Attribute("Price", priceValue));
                }
                Pattern phones = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"); //Extract phone number
                Matcher m = phones.matcher(whole_text);
                if(m.find()){
                    String phoneNumber = m.group().trim().replaceAll("[^0-9]", "").replaceFirst("(\\d{3})(\\d{3})(\\d+)", "($1) $2 – $3");
                    article.addAttribute(new Attribute("Phone", phoneNumber));
                }

            }
            StringBuilder markdown = new StringBuilder();
            Elements paragraphs = body.select("p, h3");
            for (Element element : paragraphs) {
                if (element.tagName().equalsIgnoreCase("p")) {
                    String text = Andromeda.tools.htmlEscape(element.text().replaceAll("\\s+", " ").trim());
                    if(!text.isEmpty()){
                        markdown.append("  \n").append(text).append("  \n");
                    }
                } else if (element.tagName().equalsIgnoreCase("h3")) {
                    String headerText = Andromeda.tools.htmlEscape(element.text().trim());
                    markdown.append("##### ").append(headerText).append("  \n");
                }
            }
            article.setContent(Andromeda.tools.escape(markdown.toString().replaceAll("\\[.*?]", "").trim()));
            return article;
        }

        public ArrayList<String> getLinks() {
            return links;
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
                Log.error("HOST BLOCKED : " + host);
            }
        }

        public boolean isAllowed(String path) {
            return disallowed.stream().noneMatch(path::startsWith);
        }
    }

    public static ArrayList<String> extractLinks(Elements elements, String root){
        ArrayList<String> links = new ArrayList<>();
        elements.forEach(el -> {
            String fxd = fixLink(root, el.attr("href"));
            if(Asset.isValidLink(fxd)){
                links.add(fxd);
            }
        });
        return links;
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
}