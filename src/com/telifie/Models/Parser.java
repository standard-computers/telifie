package com.telifie.Models;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Unit;
import com.telifie.Models.Clients.Sql;
import com.telifie.Models.Connectors.Radar;
import com.telifie.Models.Utilities.Console;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Network.Network;
import org.json.JSONObject;
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
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.telifie.Models.Actions.Search;

public class Parser {

    private static ArticlesClient articles;

    public Parser(Session session){
        articles = new ArticlesClient(session);
    }

    public void reparse(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ArticlesClient articles =  new ArticlesClient(new Session("telifie", "telifie"));
        Log.message("STARTING REPARSE", "PARx011");
        ArrayList<Article> parsing = articles.withProjection(
                new org.bson.Document("$or", Arrays.asList(
                    new org.bson.Document("link", Search.pattern("https://")),
                    new org.bson.Document("link", Search.pattern("http://")),
                    new org.bson.Document("source.url", Search.pattern("http://")),
                    new org.bson.Document("source.url", Search.pattern("http://"))
                )),
                new org.bson.Document("link", 1).append("link", 1)
        );
        Console.log("RE-PARSE TOTAL : " + parsing.size());
        parsing.forEach(a -> {
            Future<Article> future = executor.submit(() -> engines.fetch(a.getLink(), 0, true));
            try {
                Article ab = future.get();
                reparse();
            } catch (InterruptedException | ExecutionException e) {
                Log.error("FAILED ASYNC QUEUE TASK", "PARx010");
            } finally {
                executor.shutdown();
            }
        });
    }

    public Article parse(String uri){
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

        public static Article crawler(String uri, boolean allowExternalCrawl){
            return Parser.engines.fetch(uri, 0, allowExternalCrawl);
        }

        private static Article fetch(String url, int depth, boolean allowExternalCrawl){
            depth++;
            if(new Sql().isParsed(url)){
                Console.log("ALREADY PARSED : " + url);
                return null;
            }
            try {
                String host = Network.url(url).getProtocol() + "://" + Network.url(url).getHost();
                Connection.Response response = Jsoup.connect(url).userAgent("telifie/1.0").execute();
                if(response.statusCode() == 200){
                    Log.message("PARSING : " + url, "PARx102");
                    new Sql().parsed("telifie", url);
                    webpage wp = new webpage();
                    Article article = wp.extract(url, response.parse());
                    if(articles.lookup(article)){
                        //TODO update article
                    }else{
                        articles.create(article);
                        Console.log("ARTICLE CREATED : " + url);
                    }
                    ArrayList<String> links = wp.links;
                    int fd = depth;
                    links.forEach(link -> {
                        String href = Network.fixLink(host, link.split("\\?")[0]);
                        if(Asset.isWebpage(href) && !Andromeda.tools.contains(new String[]{"facebook.com", "instagram.com", "spotify.com", "linkedin.com", "youtube.com", "pinterest.com", "twitter.com", "tumblr.com", "reddit.com"}, href)){
                            if((allowExternalCrawl && !href.contains(host)) || (!allowExternalCrawl && href.contains(host))){
                                try {
                                    Thread.sleep(2000);
                                    if(fd <= 3){
                                        fetch(href, fd, allowExternalCrawl);
                                    }
                                } catch (InterruptedException e) {
                                    Console.log("Failed to sleep thread?");
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

        public static Article website(String url){
            try {
                Connection.Response response = Jsoup.connect(url).userAgent("telifie/1.0").execute();
                Log.message("PARSING : " + response.statusCode() + " : " + url, "PARx103");
                if(response.statusCode() == 200){
                    Document root = response.parse();
                    return new webpage().extract(url, root);
                }
                Log.error(response.statusCode() + " : " + url, "PARx113");
                return null;
            } catch (IOException e) {
                Log.error("FAILED CONNECTING TO HOST : " + url, "PARx123");
                return null;
            }
        }

        public static ArrayList<Article> batch(String uri, boolean insert){
            try {
                URL url = Network.url(uri);
                InputStream inputStream = url.openStream();
                Files.copy(inputStream, Paths.get(Telifie.configDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1]), StandardCopyOption.REPLACE_EXISTING);
                ArrayList<String[]> lines = new ArrayList<>();
                try (CSVReader reader = new CSVReader(new FileReader(Telifie.configDirectory() + "temp/" + url.getPath().split("/")[url.getPath().split("/").length - 1]))) {
                    String[] fields;
                    while ((fields = reader.readNext()) != null) {
                        lines.add(fields);
                    }
                    Log.message("PARSING CSV BATCH UPLOAD", "PARx104");
                } catch (IOException | CsvException e) {
                    Log.error("FAILED CSV FILE READ : PARSER / BATCH", "PARx114");
                }
                ArrayList<Article> parsed = new ArrayList<>();
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
                String bid = String.valueOf(Telifie.epochTime());
                for (int i = 1; i < lines.size(); i++) {
                    String[] articleData = lines.get(i);
                    Article article = new Article();
                    article.addAttribute(new Attribute("*batch", bid));
                    for (int g = 0; g < articleData.length; g++) {
                        String value = articleData[g];
                        if (g == titleIndex) {
                            article.setTitle(Andromeda.tools.escape(value));
                        } else if (g == descriptionIndex) {
                            article.setDescription(value);
                        } else if (g == linkIndex) {
                            article.setLink(value);
                        } else if (g == contentIndex) {
                            article.setContent(Andromeda.tools.escape(value));
                        } else if(g == iconIndex){
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
                        if(!article.getLink().isEmpty()){
                            String l = Network.decode(article.getLink());
                            try {
                                Thread.sleep(2500);
                                Article pa = Parser.engines.website(l);
                                //TODO more attributes, icon,
                                article.setContent(pa.getContent());
                                String[] tags = articleData[2].split(",");
                                for (String tag : tags) {
                                    article.addTag(tag.trim().toLowerCase());
                                }
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    parsed.add(article);
                    if(insert){
                        Console.log("Article created with batch parser");
                        articles.create(article);
                    }
                }
                return parsed;
            } catch (IOException e) {
                Log.error("FAILED BATCH UPLOAD", "PARx124");
            }
            return null;
        }

        public static Article image(String uri){
            Article a = new Article();
            a.setIcon(uri);
            a.setLink(uri);
            a.setDescription("Image");
            return a;
        }

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

        public ArrayList<String> links;
        private String root;

        public Article extract(String url, Document document){
            Article article = new Article();
            URL rootUri = Network.url(url);
            root = rootUri.getProtocol() + "://" + rootUri.getHost() + "/";
            article.setTitle(Andromeda.tools.htmlEscape(document.title()));
            article.setLink(url);
            document.getElementsByTag("meta").forEach(tag -> {
                String mtn = tag.attr("name");
                String mtc = Andromeda.tools.htmlEscape(tag.attr("content"));
                if (mtn.equals("keywords")) {
                    String[] words = mtc.split(",");
                    for (String word : words) {
                        article.addTag(word.trim().toLowerCase());
                    }
                }
            });
            links = Network.extractLinks(document.getElementsByTag("a"), root);
            if(!links.isEmpty()){
                links.forEach(link -> {
                    if(Andromeda.tools.contains(new String[]{"dribbble.com", "facebook.com", "instagram.com", "spotify.com", "linkedin.com", "youtube.com", "pinterest.com", "github.com", "twitter.com", "tumblr.com", "reddit.com"}, link)){
                        try {
                            String domain = new URI(link).getHost();
                            String k = (domain.split("\\.")[0].equals("www") ? domain.split("\\.")[1] : domain.split("\\.")[0]);
                            k = k.substring(0, 1).toUpperCase() + k.substring(1);
                            String[] l = link.split("\\?")[0].split("/");
                            String un = l[l.length - 1].trim();
                            article.addAttribute(new Attribute(k, un));
                        } catch (URISyntaxException e) {
                            Log.error("FAILED URI TO URL : " + link, "PARx110");
                        }
                    }
                });
            }
            document.getElementsByTag("link").forEach(linkTag -> {
                String rel = linkTag.attr("rel");
                String href = linkTag.attr("href");
                if(rel.contains("icon") && !url.contains("/wiki")){
                    article.setIcon(Network.fixLink("https://" + Network.url(url).getHost(), href));
                }
            });
            document.getElementsByTag("img").forEach(image -> {
                String src = Network.fixLink(url, image.attr("src"));
                if(!src.isEmpty() && !src.startsWith("data:") && articles.withLink(src) == null){
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
                            if (!caption.isEmpty() && caption.length() < 100) {
                                ia.setTitle(caption);
                            } else {
                                ia.setTitle(article.getTitle());
                                ia.setContent(caption);
                            }
                            ia.setLink(src);
                            ia.setIcon(src);
                            ia.setTags(article.getTags());
                            ia.setDescription("Image");
                            ia.addAttribute(new Attribute("Width", d[0] + "px"));
                            ia.addAttribute(new Attribute("Height", d[1] + "px"));
                            ia.addAttribute(new Attribute("Size", ass.fileSize()));
                            ia.addAttribute(new Attribute("File Type", ass.getExt()));
                            Article source = articles.withLink(root);
                            if (source != null) {
                                ia.setSource(new Article.Source(source.getIcon(), source.getTitle(), url));
                            } else {
                                ia.setSource(new Article.Source(article.getIcon(), article.getTitle(), url));
                            }
                            if(articles.create(ia)){
                                Console.log("IMAGE CREATED : https://telifie.com/articles/" + ia.getId());
                            }else{
                                Console.log("NOT HAPPENING, MAY EXIST");
                            }
                        }
                    });
                }
            });
            Element body = document.getElementsByTag("body").get(0);
            Element infobox = body.selectFirst(".infobox");
            body.select("table, script, header, style, img, svg, button, label, form, input, aside, code, footer, nav").remove();
            if(url.contains("/wiki")){
                article.setSource(new Article.Source("https://telifie-static.nyc3.cdn.digitaloceanspaces.com/mirror/uploads/sources/wikipedia.png", "Wikipedia", article.getLink().trim()));
                article.setLink(null);
                article.setTitle(article.getTitle().replaceAll(" - Wikipedia", ""));
                if (infobox != null) {
                    Elements infoboxRows = infobox.select("tr");
                    for (Element row : infoboxRows) {
                        Element label = row.selectFirst("th.infobox-label");
                        Element data = row.selectFirst("td.infobox-data");
                        if (label != null && data != null) {
                            String key = Andromeda.tools.sentenceCase(label.text().trim());
                            String value = data.text().replaceAll("\\[.*?]", "").trim();
                            if(!key.equalsIgnoreCase("references")){
                                if(value.contains("{") || value.contains("}")){
                                    article.addAttribute(new Attribute(key, Andromeda.tools.escape(value)));
                                }else{
                                    article.addAttribute(new Attribute(key, Andromeda.tools.htmlEscape(Andromeda.tools.sentenceCase(value))));
                                }
                            }
                        }
                    }
                }
                body.select("div.mw-jump-link, div#toc, div.navbox, table.infobox, div.vector-body-before-content, div.navigation-not-searchable, div.mw-footer-container, div.reflist, div#See_also, h2#See_also, h2#References, h2#External_links").remove();
            }
            String whole_text = document.text().replaceAll("[\n\r]", " ");
            String[] keywords = new Unit(whole_text).keywords(15);
            for(String kw : keywords){
                article.addTag(kw);
            }
            Pattern pattern = Pattern.compile("\\$\\d+(\\.\\d{2})?");
            Matcher matcher = pattern.matcher(whole_text);
            if (matcher.find()) {
                article.addAttribute(new Attribute("Price", matcher.group()));
            }
            Pattern phones = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
            Matcher m = phones.matcher(whole_text);
            if(m.find() && !url.contains("/wiki")){
                String phoneNumber = m.group().trim().replaceAll("[^0-9]", "").replaceFirst("(\\d{3})(\\d{3})(\\d+)", "($1) $2 - $3");
                article.addAttribute(new Attribute("Phone", phoneNumber));
            }
            Pattern emails = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,6}", Pattern.CASE_INSENSITIVE);
            Matcher em = emails.matcher(whole_text);
            if (em.find()) {
                article.addAttribute(new Attribute("Email", em.group().trim()));
            }
            Pattern addressPattern = Pattern.compile(
                    "\\b\\d+\\s+([A-Za-z0-9.\\-'\\s]+)\\s+" +
                            "(St\\.?|Street|Rd\\.?|Road|Ave\\.?|Avenue|Blvd\\.?|Boulevard|Ln\\.?|Lane|Dr\\.?|Drive|Ct\\.?|Court)\\s+" +
                            "(\\w+),\\s+" +
                            "(Ohio|OH|Ala|AL|Alaska|AK|Ariz|AZ|Ark|AR|Calif|CA|Colo|CO|Conn|CT|Del|DE|Fla|FL|Ga|GA|Hawaii|HI|Idaho|ID|Ill|IL|Ind|IN|Iowa|IA|Kans|KS|Ky|KY|La|LA|Maine|ME|Md|MD|Mass|MA|Mich|MI|Minn|MN|Miss|MS|Mo|MO|Mont|MT|Nebr|NE|Nev|NV|N\\.H\\.|NH|N\\.J\\.|NJ|N\\.M\\.|NM|N\\.Y\\.|NY|N\\.C\\.|NC|N\\.D\\.|ND|Okla|OK|Ore|OR|Pa|PA|R\\.I\\.|RI|S\\.C\\.|SC|S\\.D\\.|SD|Tenn|TN|Tex|TX|Utah|UT|Vt|VT|Va|VA|Wash|WA|W\\.Va|WV|Wis|WI|Wyo|WY)\\s+" +
                            "(\\d{5}(?:[-\\s]\\d{4})?)", Pattern.CASE_INSENSITIVE);
            Matcher am = addressPattern.matcher(whole_text);
            while (am.find()) {
                String fullAddress = am.group(0);
                article.setDescription("Building");
                if(article.hasAttribute("Address")){

                }else{
                    try {
                        JSONObject location = Radar.get(fullAddress);
                        article.addAttribute(new Attribute("Longitude", String.valueOf(location.getFloat("longitude"))));
                        article.addAttribute(new Attribute("Latitude", String.valueOf(location.getFloat("latitude"))));
                        article.addAttribute(new Attribute("Address", location.getString("formattedAddress")));
                    } catch (IOException | InterruptedException | NullPointerException e) {
                        Log.error("RADAR PACKAGE ERROR", "PARx112");
                    }
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
            String prtxt = markdown.toString().replaceAll("\\[.*?]", "").trim();
            Pattern dates = Pattern.compile("\\b(\\d{1,2})\\s(January|February|March|April|May|June|July|August|September|October|November|December)\\s(\\d{4})\\b");
            Matcher dm = dates.matcher(prtxt);
            StringBuilder sb = new StringBuilder();
            while (dm.find()) {
                String replacement = dm.group(2) + " " + dm.group(1) + ", " + dm.group(3);
                dm.appendReplacement(sb, replacement);
            }
            dm.appendTail(sb);
            prtxt = sb.toString();
            article.setContent(Andromeda.tools.escape(prtxt));
            return article;
        }
    }
}