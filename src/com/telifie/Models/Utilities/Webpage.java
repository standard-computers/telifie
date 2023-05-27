package com.telifie.Models.Utilities;

import com.telifie.Models.Article;
import com.telifie.Models.Articles.Attribute;
import com.telifie.Models.Articles.Image;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;

public class Webpage {

    public static Article extract(String url, Document document){

        Article article = new Article();
        Elements metaTags = document.getElementsByTag("meta");
        for (Element tag : metaTags){
            String mtn = tag.attr("name");
            String mtc = tag.attr("content");
            if(mtn.equals("description")){
                if(!tag.attr("content").trim().equals("")){
                    article.setContent(mtc);
                }
            }else if(mtn.equals("keywords")){
                String[] words = mtc.split(",");
                for(String word : words){
                    article.addTag(word.trim().toLowerCase());
                }
            }else if(mtn.equals("og:image")){
                article.addImage(new Image(mtc, "", url));
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

        //Parse images
        Elements images = document.getElementsByTag("img");
        for(Element image : images){

            String src = Telifie.tools.detector.fixLink(url, image.attr("src"));
            String srcset = Telifie.tools.detector.fixLink(url, image.attr("srcset"));
            if(!src.equals("") && !src.equals("null") && Telifie.tools.detector.getType(src).equals("image") && !image.attr("src").trim().toLowerCase().startsWith("data:")){

                String caption = Telifie.tools.strings.htmlEscape(image.attr("alt").replaceAll("“", "").replaceAll("\"", "&quote;"));
                Image img = new Image(src, caption, url);
                article.addImage(img);
            }else if(!srcset.equals("") && !srcset.startsWith("data:")){

                String link = "https://" + srcset.split("\\s")[0];
                String caption =  Telifie.tools.strings.htmlEscape(image.attr("alt").replaceAll("“", "").replaceAll("\"", "&quote;"));
                Image img = new Image(src, caption, url);
                article.addImage(img);
            }
        }
        article.setTitle(Telifie.tools.strings.escape(document.title()));
        article.setLink(url);
        String whole_text = document.text().replaceAll("[\n\r]", " ");

        Matcher phone_numbers = Telifie.tools.detector.findPhoneNumbers(whole_text);
        while(phone_numbers.find()){
            String phone_number = phone_numbers.group().trim().replaceAll("[^0-9]", "").replaceFirst("(\\d{3})(\\d{3})(\\d+)", "($1) $2 – $3");
            Attribute attr = new Attribute("Phone", phone_number);
            article.addAttribute(attr);
        }

        if(article.getContent() == null || article.getContent().equals("")){
            Element body = document.getElementsByTag("body").get(0);
            body.select("table, script, header, style, img, svg, button, label, form, input, aside, code, nav").remove();
//          TODO Convert tables to datasets
            if(url.contains("wiki")){
                article.setTitle(article.getTitle().replaceAll(" - Wikipedia", ""));
                body.select("div.mw-jump-link, div#toc, div.navbox, table.infobox, div.vector-body-before-content, div.navigation-not-searchable, div.mw-footer-container, div.reflist, div#See_also, h2#See_also, h2#References, h2#External_links").remove();
            }
            // Convert <p> elements to newlines in Markdown
            StringBuilder markdown = new StringBuilder();
            Elements paragraphs = body.select("p, h2"); // Select both paragraphs (p) and H2 headers (h2)
            for (Element element : paragraphs) {
                if (element.tagName().equalsIgnoreCase("p")) {
                    String text = element.text().trim();
                    if(!text.equals("")){
                        markdown.append(text).append("\\n\\n");
                    }
                } else if (element.tagName().equalsIgnoreCase("h2")) {
                    String headerText = element.text().trim();
                    markdown.append("### ").append(headerText).append("\\n\\n"); // Append H2 headers as ## Header Text
                }
            }
            String md = Telifie.tools.strings.htmlEscape(markdown.toString().replaceAll("\\s+", " ").replaceAll("\\[.*?]", "").trim());
            article.setContent(md);
        }
        return article;
    }
}
