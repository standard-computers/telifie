package com.telifie.Models.Utilities;

import com.telifie.Models.Article;
import com.telifie.Models.Articles.Attribute;
import com.telifie.Models.Articles.Image;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.regex.Matcher;

public class DocumentExtract {

    private static Document document;

    public DocumentExtract(Document root){
        DocumentExtract.document = root;
    }


    public static Article extract(String url){

        Article article = new Article();

        //Parse header tags
        Elements metaTags = document.getElementsByTag("meta");
        for (Element tag : metaTags){

            String metaTagName = tag.attr("name");
            String metaTagContent = tag.attr("content");

            //Get description as first content block
            if(metaTagName.equals("description")){

                if(!tag.attr("content").trim().equals("")){

                    article.setContent(metaTagContent);
                }
            }
            //Get keywords as first tags if any
            else if(metaTagName.equals("keywords")){

                ArrayList<String> tags = new ArrayList<>();
                String[] words = metaTagContent.split(",");
                for(String word : words){
                    tags.add(word.trim().toLowerCase());
                }
                article.setTags(tags);
            }
            //Get icon if possible
            else if(metaTagName.equals("theme-color")){

                article.addAttribute(new Attribute("Color", metaTagContent));
            }else if(metaTagName.equals("og:image")){

                article.addImage(new Image(metaTagContent, "", url));
            }
            //Is mobile friendly
            else if(metaTagName.equals("viewport")){

                article.addAttribute(new Attribute("Mobile Friendly", "Yes"));
            }
        }

        Elements linkTags = document.getElementsByTag("link");
        for (Element linkTag : linkTags){

            String rel = linkTag.attr("rel");
            String href = linkTag.attr("href");
            String linkType = linkTag.attr("type");
            if(rel.contains("icon")){
                article.setIcon(href);
            }
        }

        //Parse images
        Elements images = document.getElementsByTag("img");
        for(Element image : images){

            String src = Tool.fixLink(url, image.attr("src"));
            String srcset = Tool.fixLink(url, image.attr("srcset"));
            if(!src.equals("") && !src.equals("null") && Tool.getType(src).equals("image")){

                String caption =  image.attr("alt");
                Image img = new Image(src, caption, url);
                article.addImage(img);
            }else if(srcset != null && !srcset.equals("")){

                String link = "https://" + srcset.split("\\s")[0];
                String caption =  image.attr("alt");
                Image img = new Image(src, caption, url);
                article.addImage(img);
            }
        }
        article.setTitle(Tool.escape(document.title()));
        article.setLink(url);


        String whole_text = Tool.escape(document.text().replaceAll("[\n\r]", " "));
        //Work on possible attributes
        //.replaceAll("\\s+", " ")
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


        if(article.getContent() == null || article.getContent().equals("")){
            Element body = document.getElementsByTag("body").get(0);
            body.getElementsByTag("aside").remove();
            Element main = (body.getElementsByTag("main") == null || body.getElementsByTag("main").size() < 1 ? null : body.getElementsByTag("main").get(0));
            if(main == null){

                // Convert HTML to Markdown
                article.setContent(Tool.convertHtmlToMarkdown(Tool.extractBodyContent(body.toString())));
            }else{

                article.setContent(Tool.convertHtmlToMarkdown(Tool.extractBodyContent(main.toString())));
            }
        }
        return article;
    }
}
