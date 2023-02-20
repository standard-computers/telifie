package com.telifie.Models;

import com.telifie.Models.Utilities.*;
import com.telifie.Models.Actions.Out;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;

public class Parser {

    private final String uri;
    private final ArrayList<Article> traversable = new ArrayList<>();
    private final ArrayList<String> parsed = new ArrayList<>();
    private final int MAX_DEPTH = 1;

    public Parser(String uri){
        this.uri = uri;
    }

    public ArrayList<Article> getTraversable() {
        return traversable;
    }

    public ArrayList<String> getParsed() {
        return parsed;
    }

    public int getMAX_DEPTH() {
        return MAX_DEPTH;
    }

    public Article parse(){
        if(this.isUrl()){ //Crawl website if url

            return crawl(uri, 0);

        }else if(this.isFile()){ //Parsing a file

            File file = new File(this.uri);
            if(file.exists()){

                String file_type = this.getType();
                String file_extension = this.getExtension();

            }else{
                Out.error("[FILE NOT FOUND] " + uri);
            }

        }

        return null;

    }

    private Article crawl(String url, int depth){
        if(depth > MAX_DEPTH){
            return null;
        }
        parsed.add(url);
        Connection.Response response = null;
        try {
            response = Jsoup.connect(url)
                    .timeout(5000)
                    .method(Connection.Method.HEAD)
                    .execute();
            if(response.statusCode() == 200){

                Document root = Jsoup.connect(url).get();
                int statusCode = root.connection().response().statusCode();
                if(statusCode == 200){
                    //TODO check page status
                }else{
                    Out.console("Status Code: " + statusCode);
                }
                Article article = new Article();
                article.setTitle(root.title());
                article.setLink(url);

                //Parse header tags
                Elements meta_tags = root.getElementsByTag("meta");
                for (Element tag : meta_tags){

                    //Get description as first content block
                    if(tag.attr("name").equals("description")){
                        article.setContent(tag.attr("content"));
                    }
                    //Get keywords as first tags if any
                    else if(tag.attr("name").equals("keywords")){
                        ArrayList<String> tags = new ArrayList<>();
                        String[] words = tag.attr("content").split(",");
                        for(String word : words){
                            tags.add(word.trim().toLowerCase());
                        }
                        article.setTags(tags);
                    }
                }

                //Parse images
                Elements images = root.getElementsByTag("img");
                for(Element image : images){

                    String src = Tool.fixLink(url, image.attr("src"));
                    if(!src.equals("") && !src.equals("null") && Tool.getType(src).equals("image")){

                        String caption =  image.attr("alt");
                        if(caption.equals("")){
                            //TODO auto tag/identify with OpenCV
                        }
                        Image img = new Image(src, caption, url);
                        article.addImage(img);

                    }

                }

                String whole_text = root.text();
                //TODO generate tags based on content of page

                //Work on possible attributes
                Matcher phone_numbers = Tool.findPhoneNumbers(whole_text);
                while(phone_numbers.find()){
                    String phone_number = phone_numbers.group().trim().replaceAll("[^0-9]", "").replaceFirst("(\\d{3})(\\d{3})(\\d+)", "($1) $2 – $3");
                    Attribute attr = new Attribute("Phone", phone_number);
                    article.addAttribute(attr);
                }

                //Pulling emails
                Matcher emails = Tool.findEmails(whole_text);
                while(emails.find()){

                    Attribute attr = new Attribute("Email", emails.group().toLowerCase());
                    article.addAttribute(attr);

                }

                //Pulling addresses
                Matcher addresses = Tool.findAddresses(whole_text);
                while(addresses.find()){
                    Attribute attr = new Attribute("Address", addresses.group().toLowerCase());
                    article.addAttribute(attr);
                }

                Elements links = root.getElementsByTag("a");

                Association pages = new Association("https://telifie-static.nyc3.cdn.digitaloceanspaces.com/wwdb/pages.png", "Pages");

                if(links.size() > 0){

                    for(Element link : links){

                        String page = link.attr("href"), fixed_url = Tool.fixLink(uri, page);

                        if(!page.equals(url) && !page.equals("") && !page.startsWith("tel:") && !page.startsWith("mailto:") && !this.isParsed(fixed_url) && !page.equals("/") && !page.startsWith("#") && !page.equals(uri)){ //So long as it's not original URL or hasn't been parsed yet

                            if(page.startsWith("/") || page.startsWith(url) || page.contains(fixed_url)){ //Make sure their actual child pages, not links out
                                Article child = crawl(fixed_url, depth + 1);

                                if(child != null){
                                    Child child_association = new Child(child.getId(), child.getIcon(), child.getTitle(), child.getDescription());
                                    child_association.setId(child.getId());
                                    pages.addArticle(child_association);
                                }

                            }

                        }

                    }

                    if(pages.size() >= 1){
                        article.addAssociation(pages);
                    }

                }

                Out.console(article.toString());
                this.traversable.add(article); //Push new articles to traversable for upload.
                return article;

            }else{
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    public boolean isUrl(){

        return this.uri.startsWith("https://") || this.uri.startsWith("http://") || this.uri.startsWith("www");

    }

    public boolean isFile(){

        return this.uri.startsWith("file://") || this.uri.startsWith("c:/") || this.uri.startsWith("\\");

    }

    public String getType(){
        if(this.isUrl() || this.uri.endsWith("html")){
            return "webpage";
        }else if(this.uri.endsWith("png") || this.uri.endsWith("gif") || this.uri.endsWith("jpeg") || this.uri.endsWith("jpg") || this.uri.endsWith("psd")){
            return "image";
        }else if(this.uri.endsWith("mp4") || this.uri.endsWith("wmv") || this.uri.endsWith("mov") || this.uri.endsWith("avi") || this.uri.endsWith("flv") || this.uri.endsWith("mkv")){
            return "video";
        }else if(this.uri.endsWith("wav") || this.uri.endsWith("mp3")){
            return "audio";
        }else if(this.uri.endsWith("pdf") || this.uri.endsWith("docx") || this.uri.endsWith("txt") || this.uri.endsWith("rtf")){
            return "document";
        }else if(this.uri.endsWith("php") || this.uri.endsWith("css")){
            return "code";
        }else{
            return "Unknown";
        }
    }

    public String getExtension(){
        return this.uri.split("\\.")[this.uri.split("\\.").length - 1];
    }

    public boolean isParsed(String uri){
        for (String s : this.parsed) {
            if (s.equals(uri)) {
                return true;
            }
        }
        return false;
    }

}
