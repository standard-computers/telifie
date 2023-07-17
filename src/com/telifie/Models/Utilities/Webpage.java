package com.telifie.Models.Utilities;

import com.telifie.Models.Article;
import com.telifie.Models.Articles.Attribute;
import com.telifie.Models.Articles.Image;
import com.telifie.Models.Articles.Source;
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
        article.setTitle(Telifie.tools.strings.escape(document.title()));
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
                                article.getLink()
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