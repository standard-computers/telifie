package com.telifie.Models;

import com.telifie.Models.Utilities.*;
import com.telifie.Models.Actions.Out;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

public class Parser {

    private static String uri;
    private static final ArrayList<Article> traversable = new ArrayList<>();
    private static final ArrayList<String> parsed = new ArrayList<>();
    private static final int MAX_DEPTH = 1;

    public static class engines {

        public static Article parse(String uri){
            if(Tool.isUrl(uri)){ //Crawl website if url
                //Is it a file though and not a website or both
                //TODO change parsing based on uri/url extension
                //TODO check url against articles domain parsing is happening under
                //TODO Download anyways
                //TODO firewall check on url
                return Parser.crawl(uri, 0);
            }else if(Tool.isFile(uri)){ //Parsing a file

                File file = new File(uri);
                if(file.exists()){

                }else{
                    Out.error("[FILE NOT FOUND] " + uri);
                }
            }

            return null;
        }

        /**
         * Parses articles for csv rows
         * Provide csv file location. <a href=''>See template</a>
         * @param uri Location of csv on disk
         * @param delimiter CSV data delimiter
         * @return ArrayList<Article> List of Articles
         */
        public static ArrayList<Article> batch(String uri, String delimiter){
            if(!new File(uri).exists()){
                return null;
            }

            if(uri.endsWith("csv")){

                ArrayList<String[]> lines = new ArrayList<>();
                try (BufferedReader br = new BufferedReader(new FileReader(uri))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] fields = line.split(delimiter);
                        lines.add(fields);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ArrayList<Article> articles = new ArrayList<>();
                String[] headers = lines.get(0);
                int titleIndex = 0, descriptionIndex = 0, linkIndex = 0, contentIndex = 0;
                for (int i = 0; i < headers.length; i++){
                    String hV = lines.get(0)[i].toLowerCase().trim();
                    if(hV.equals("title")){
                        titleIndex = i;
                    }else if(hV.equals("description")){
                        descriptionIndex = i;
                    }else if(hV.equals("link")){
                        linkIndex = i;
                    }else if(hV.equals("content")){
                        contentIndex = i;
                    }
                }
                for (int i = 1; i < lines.size(); i++) {

                    String[] articleData = lines.get(i);
                    Article article = new Article();
                    for(int g = 0; g < articleData.length; g++){

                        String value = articleData[g];
                        if(g == titleIndex){

                            article.setTitle(value);
                        }else if(g == descriptionIndex){

                            article.setDescription(value);
                        }else if(g == linkIndex){

                            article.setLink(value);
                        }else if(g == contentIndex){

                            article.setContent(value);
                        }else{ //Not specified value

                            //Do special stuff with attributes
                            article.addAttribute(new Attribute(headers[g], value));
                        }
                    }
                    articles.add(article);
                }

                return articles;
            }else{

                return null;
            }
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
         * Parse plain text into an article
         * @param text Plain text as string to parse
         * @return
         */
        public static Article text(String text){

            return null;
        }

        /**
         * Parsing code files into article
         * This could be things like css, html, js, cpp, etc.
         * @param uri Location of asset on disk
         * @return Article representation of asset
         */
        public static Article code(String uri){

            return null;
        }
    }

    /**
     * Parser.connectors is for parsing content from connectors
     */
    public static class connectors {

        /**
         * Returns article for wikipedia page given title
         * Title must be the title for the API reference in the URL
         * @param title String title of wikipedia article
         * @return Article of Wikipedia
         */
        public static Article wikipedia(String title) {

            return null;
        }
    }

    /**
     * The index is the knowledge base to parse assets against
     * Parser.index is an objectification of this
     * There are separate indexes created for each domain
     */
    public static class index {

        private static String workingDirectory;

        public index(){
            String operatingSystem = System.getProperty("os.name");
            if(operatingSystem.equals("Mac OS X")){

                workingDirectory = Out.MAC_SYSTEM_DIR;
            }else if(operatingSystem.startsWith("Windows")){

                workingDirectory = Out.WINDOWS_SYSTEM_DIR;
            }else{

                workingDirectory = Out.UNIX_SYSTEM_DIR;
            }
        }

        public static void add(Vars.Languages language){

        }

        /**
         * Subclass if Parser.index for dictionary of correctly spelled acceptable words
         */
        public static class dictionary {

            private static File dictionaryFile;
            private static List<String> words = new ArrayList<>();

            /**
             * Select the dictionary using preferred language Vars.Language.LANGUAGE
             * @param language
             */
            public dictionary(Vars.Languages language){
                File dictionaryDir = new File(index.workingDirectory + "/dictionary/");
                if(!dictionaryDir.exists()){
                    dictionaryDir.mkdirs();
                }
                dictionaryFile = new File(index.workingDirectory + "/dictionary/" + language + ".txt");

                //Load words already in dictionary
                String dict = Tool.fileToString(dictionaryFile.getAbsolutePath());
                String[] dictWords = dict.split("\\s+");
                for(String word : dictWords){
                    if(!word.trim().equals("")){
                        words.add(word);
                    }
                }
                Out.console("Read In -> " + words.toString());
            }

            /**
             * Adds array of String words to the onboard dictionary
             * @param newWords
             */
            public static void add(String[] newWords){
                for(String word : newWords){
                    if(!words.contains(word.trim().toLowerCase())){
                        words.add(word.trim().toLowerCase());
                    }
                }
                save();
            }

            /**
             * Given a word as String, returns if exists in the provided dictionary
             * @param req Word
             * @return boolean True or False if the word exists in the provided dictionary
             */
            public static boolean exists(String req){
                for(String word : words){
                    if(word.equals(req)){
                        return true;
                    }
                }
                return false;
            }

            /**
             * Saves dictionary
             */
            private static void save(){
                Collections.sort(words);
                String dict = "";
                for(String word : words){
                    dict = dict + word + " ";
                }
                try {
                    FileWriter fileWriter = new FileWriter(dictionaryFile);
                    fileWriter.write(dict);
                    fileWriter.close();
//                    words.removeAll(words);
                    Out.console("Dictionary updated");
                } catch (IOException e) {
                    Out.console("Failed to update dictionary");
                    e.printStackTrace();
                }
            }

            /**
             * Returns length of dictionary
             * @return
             */
            public static int getSize(){

                return words.size();
            }
        }
    }

    private static Article crawl(String url, int depth){
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

                        if(!page.equals(url) && !page.equals("")
                                && !page.startsWith("tel:") && !page.startsWith("mailto:")
                                && !Parser.isParsed(fixed_url) && !page.equals("/")
                                && !page.startsWith("#") && !page.equals(uri)) { //So long as it's not original URL or hasn't been parsed yet

                            if (page.startsWith("/") || page.startsWith(url) || page.contains(fixed_url)) { //Make sure their actual child pages, not links out
                                Article child = crawl(fixed_url, depth + 1);

                                if (child != null) {
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
                Parser.traversable.add(article); //Push new articles to traversable for upload.
                return article;

            }else{
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    public ArrayList<Article> getTraversable() {
        return traversable;
    }

    public static void setUri(String uri){
        Parser.uri = uri;
    }

    public static String getUri(){
        return uri;
    }

    public static boolean isParsed(String uri){
        for (String s : Parser.parsed) {
            if (s.equals(uri)) {
                return true;
            }
        }
        return false;
    }
}
