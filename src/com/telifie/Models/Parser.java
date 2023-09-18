package com.telifie.Models;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.telifie.Models.Utilities.Console;
import com.telifie.Models.Utilities.Event;
import com.telifie.Models.Articles.*;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.TimelinesClient;
import com.telifie.Models.Utilities.*;
import org.apache.commons.text.StringEscapeUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private static String uri, host;
    private static final ArrayList<Article> traversable = new ArrayList<>();
    private static final ArrayList<String> parsed = new ArrayList<>();
    private static ArticlesClient articles;

    public Parser(Configuration config, Session session){
        articles = new ArticlesClient(config, session);
    }

    public static class engines {

        public static Article parse(String uri){
            traversable.removeAll(traversable);
            Parser.uri = uri;
            if(Telifie.tools.detector.isWebpage(uri)){
                try {
                    host = new URL(uri).getHost();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                return Parser.engines.website(uri);
            }else if(Telifie.tools.detector.isFile(uri)){ //File URI provided
                File file = new File(uri);
                if(file.exists()){

                }
            }else{
                Asset asset = new Asset(uri);
                asset.download();
                if(asset.getExt().equals("md")){
                    return Parser.engines.markdown(asset);
                }else if(asset.getExt().equals("txt")){
                    return Parser.engines.text(asset);
                }
            }
            return null;
        }

        public static void recursive(Configuration config, Session session, int start){
            articles = new ArticlesClient(config, session);
            TimelinesClient timelines = new TimelinesClient(config, session);
            ArrayList<Article> as = articles.linked();
            List<Article> as2 = as.subList(start, as.size());
            int i = 0;
            for(Article a : as2){
                i++;
                parsed.removeAll(parsed);
                int lastCrawl = timelines.lastEvent(a.getId(), Event.Type.CRAWL);
                System.out.println(i);
                if(lastCrawl > 3602000 || lastCrawl == -1){ //7 days
                    timelines.addEvent(a.getId(), new Event(
                            Event.Type.CRAWL,
                            "com.telifie.web-app@parser",
                            "Crawled")
                    );
                    parsed.add(a.getLink());
                    Parser.engines.crawl(a.getLink(), Integer.MAX_VALUE, false);
                }
            }
            recursive(config, session, start);
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
                    Console.out.message("Disallowed by robots.txt: " + url);
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
                        System.out.println("Created with source -> Created article " + article.getTitle());
                    }else if(article.getSource() == null && (created = articles.create(article))){
                        System.out.println("Created with link -> Created article " + article.getTitle());
                    }
                    ArrayList<Element> links = root.getElementsByTag("a");
                    for(Element link : links){
                        String href = Telifie.tools.detector.fixLink(host, link.attr("href").split("\\?")[0]);
                        if(!isParsed(href)
                                && Telifie.tools.detector.isWebpage(href)
                                && !Telifie.tools.contains(new String[]{
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
                Console.out.message("Server Response:" + response.statusCode());
                if(response.statusCode() == 200){
                    Document root = response.parse();
                    Article article = webpage.extract(url, root);
                    ArrayList<String> links = Parser.extractLinks(root.getElementsByTag("a"), uri);
                    if(!links.isEmpty()){
                        for(String link : links){
                            if(Telifie.tools.contains(new String[]{"facebook.com", "instagram.com", "spotify.com", "linkedin.com", "youtube.com", "pinterest.com", "github.com", "twitter.com", "tumblr.com", "reddit.com"}, link)){
                                URI uri;
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
                    Files.copy(inputStream, Paths.get(Telifie.configDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1]), StandardCopyOption.REPLACE_EXISTING);
                    ArrayList<String[]> lines = new ArrayList<>();
                    try (CSVReader reader = new CSVReader(new FileReader(Telifie.configDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1]))) {
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
                                if(!value.trim().isEmpty()){
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
         * Parsing docx like files into articles
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

        public static Article markdown(Asset asset){
            Article a = new Article();
            Andromeda.unit u = new Andromeda.unit(asset.getContents());
            String[] keywords = u.keywords(6);
            a.setTitle(uri.split("/")[uri.split("/").length - 1].split("\\.")[0]);
            a.setDescription("Markdown File");
            a.addTags(keywords);
            a.setContent(Telifie.tools.escapeMarkdownForJson(Telifie.tools.htmlEscape(asset.getContents())));
            a.addAttribute(new Attribute("File Type", "Markdown"));
            a.addAttribute(new Attribute("Size", asset.fileSize()));
            a.addAttribute(new Attribute("Lines", asset.lineCount()));
            a.addAttribute(new Attribute("Words", asset.wordCount()));
            return a;
        }

        public static Article text(Asset asset){
            Article a = new Article();
            Andromeda.unit u = new Andromeda.unit(asset.getContents());
            String[] keywords = u.keywords(6);
            a.setTitle(uri.split("/")[uri.split("/").length - 1].split("\\.")[0]);
            a.setDescription("Text File");
            a.addTags(keywords);
            a.setContent(Telifie.tools.escapeMarkdownForJson(Telifie.tools.htmlEscape(asset.getContents())));
            a.addAttribute(new Attribute("File Type", "Plain Text"));
            a.addAttribute(new Attribute("Size", asset.fileSize()));
            a.addAttribute(new Attribute("Lines", asset.lineCount()));
            a.addAttribute(new Attribute("Words", asset.wordCount()));
            return a;
        }
    }

    public static class webpage {
        public static Article extract(String url, Document document){
            Article article = new Article();
            article.setDescription("Webpage");
            Elements metaTags = document.getElementsByTag("meta");
            for (Element tag : metaTags){
                String mtn = tag.attr("name");
                String mtc = Telifie.tools.htmlEscape(tag.attr("content"));
                switch (mtn) {
                    case "description" -> {
                        if (!tag.attr("content").trim().isEmpty()) {
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
                if(!src.isEmpty() && !src.equals("null") && Telifie.tools.detector.getType(src).equals("image") && !image.attr("src").trim().toLowerCase().startsWith("data:")){
                    String caption = Telifie.tools.htmlEscape(image.attr("alt").replaceAll("“", "").replaceAll("\"", "&quote;").trim());
                    if(!caption.equals("Page semi-protected") && !caption.equals("Wikimedia Foundation") && !caption.equals("Powered by MediaWiki") && !caption.equals("Edit this at Wikidata") && !caption.equals("This is a good article. Click here for more information.")){
                        Image img = new Image(src, caption, url);
                        article.addImage(img);
                    }
                }else if(!srcset.isEmpty() && !srcset.startsWith("data:")){
                    String caption =  Telifie.tools.htmlEscape(image.attr("alt").replaceAll("“", "").replaceAll("\"", "&quote;"));
                    Image img = new Image(src, caption, url);
                    article.addImage(img);
                }
            }
            article.setTitle(Telifie.tools.htmlEscape(document.title()));
            article.setLink(url);
            String whole_text = document.text().replaceAll("[\n\r]", " ");
            Pattern pattern = Pattern.compile("\\$\\d+(\\.\\d{2})?");
            Matcher matcher = pattern.matcher(whole_text);
            if (matcher.find()) {
                String priceValue = matcher.group();
                article.addAttribute(new Attribute("Price", priceValue));
            }
            if(article.getContent() == null || article.getContent().isEmpty()){
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
                        if(!text.isEmpty()){
                            markdown.append("  \n").append(text).append("  \n");
                        }
                    } else if (element.tagName().equalsIgnoreCase("h3")) {
                        String headerText = Telifie.tools.escape(element.text().trim());
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

    public static ArrayList<String> extractLinks(Elements elements, String root){
        ArrayList<String> links = new ArrayList<>();
        for(Element el : elements){
            String link = el.attr("href");
            String fixed = Telifie.tools.detector.fixLink(root, link);
            if(Telifie.tools.detector.isValidLink(fixed)){
                links.add(fixed);
            }
        }
        return links;
    }
}