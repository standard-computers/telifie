package com.telifie.Models;

import com.telifie.Models.Actions.Event;
import com.telifie.Models.Articles.*;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Collectors;

public class Article {

    private String id, title, link, icon, description;
    private double priority = 1.01;
    private boolean verified = false;
    private String content;
    private ArrayList<String> tags = new ArrayList<>();
    private ArrayList<Image> images = new ArrayList<>();
    private ArrayList<Attribute> attributes = new ArrayList<>();
    private ArrayList<Association> associations = new ArrayList<>();
    private ArrayList<DataSet> dataSets = new ArrayList<>();
    private Source source;
    private final int origin;

    public Article(){
        this.id = UUID.randomUUID().toString();
        this.origin = (int) (System.currentTimeMillis() / 1000);
    }

    public Article(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? UUID.randomUUID().toString() : document.getString("id"));
        this.verified = (document.getBoolean("verified") != null && document.getBoolean("verified"));
        this.title = Telifie.tools.strings.escape(document.getString("title"));
        this.link = document.getString("link");
        this.icon = document.getString("icon");
        this.description = document.getString("description");
        this.priority = (document.getDouble("priority") == null ? 1.01 : document.getDouble("priority"));
        this.content = (document.getString("content") != null ?  MarkdownEscapeUtils.escapeMarkdownForJson(document.getString("content")) : "");
        this.origin = (document.getInteger("origin") == null ? 0 : document.getInteger("origin"));
        this.tags = document.get("tags", ArrayList.class);

        ArrayList<Document> iterable = (ArrayList<Document>) document.getList("images", Document.class);
        if (iterable != null && iterable.size() >= 1) {
            for (Document doc : iterable) {
                Image ni = new Image(doc);
                ni.setId(this.getId());
                this.addImage(new Image(doc));
            }
        }else{
            this.images = new ArrayList<>();
        }
        ArrayList<Document> it2 = (ArrayList<Document>) document.getList("attributes", Document.class);
        if (it2 != null) {
            it2.forEach(doc -> this.addAttribute(new Attribute(doc.getString("key"), doc.getString("value"))));
        }else{
            this.attributes = new ArrayList<>();
        }
        ArrayList<Document> it3 = (ArrayList<Document>) document.getList("associations", Document.class);
        if (it3 != null) {
            it3.forEach(doc -> this.addAssociation(new Association(doc)));
        }else{
            this.associations = new ArrayList<>();
        }
        ArrayList<Document> it4 = (ArrayList<Document>) document.getList("data_sets", Document.class);
        if(it4 != null){
            it4.forEach(doc -> this.addDataSet(new DataSet(doc)));
        }else{
            this.dataSets = new ArrayList<>();
        }
        Document sourceDocument = document.get("source", Document.class);
        if (sourceDocument != null) {
            this.source = new Source(sourceDocument);
        }else{
            this.source = null;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ArrayList<Image> getImages() {
        return images;
    }

    public void addImage(Image image){
        this.images.add(image);
    }

    public void addAttribute(Attribute attr){
        this.attributes.add(attr);
    }

    public boolean hasAttribute(String key){
        for(Attribute attr : this.attributes){
            if(attr.getKey().toLowerCase().trim().equals(key)){
                return true;
            }
        }
        return false;
    }

    public String getAttribute(String key){
        for(Attribute attr : this.attributes){
            if(attr.getKey().toLowerCase().trim().equals(key.toLowerCase())){
                return attr.getValue();
            }
        }
        return null;
    }

    public void addAssociation(Association ass){
        this.associations.add(ass);
    }

    public void addDataSet(DataSet dataSet){
        this.dataSets.add(dataSet);
    }

    public void setSource(Source source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                ", \"verified\" : " + verified +
                ", \"title\" : \"" + title + '\"' +
                (link == null ? "" : ", \"link\" : \"" + link + '\"') +
                (icon == null ? "" : ", \"icon\" : \"" + icon + '\"') +
                (description == null || description.equals("") ? "" : ", \"description\" : \"" + description + '\"') +
                (priority == 0 ? "" : ", \"priority\" : " + priority) +
                (content == null ? "" : ", \"content\" : \"" + content + "\"") +
                (tags == null ? "" : ", \"tags\" : " + tags.stream().map(tag -> "\"" + tag + "\"").collect(Collectors.joining(", ", "[", "]"))) +
                (images == null ? "" : ", \"images\" : " + images) +
                (attributes == null ? "" : ", \"attributes\" : " + attributes) +
                (associations.size() == 0 ? "" : ", \"associations\" : " + associations) +
                (dataSets.size() == 0 ? "" : ", \"data_sets\" : " + dataSets) +
                (source == null ? ", \"source\" : null" : ", \"source\" : " + source) +
                ", \"origin\" : " + origin +
                '}';
    }

    public JSONObject toJson(){
        return new JSONObject(this.toString());
    }

    public ArrayList<Event> compare(Article old){
        ArrayList<Event> changes = new ArrayList<>();
        Iterator<String> newKeys = this.toJson().keys();
        while (newKeys.hasNext()) {
            String key = newKeys.next();
            if(this.toJson().get(key) instanceof String){
                if(old.toJson().has(key) && !this.toJson().getString(key).equals(old.toJson().getString(key))){ //If the value at key HAS changed
                    //i.e. {"title", "old_title_value", "new_title_value"}
                    changes.add(
                            new Event(
                                    Event.Type.UPDATE,
                                    Telifie.getEpochTime(),
                                    "GUEST",
                                    key + " : " + old.toJson().getString(key) + " => " + key + " : " + this.toJson().getString(key)
                            )
                    );
                }else if(!old.toJson().has(key)){
                    changes.add(
                            new Event(
                                    Event.Type.PUT,
                                    Telifie.getEpochTime(),
                                    "GUEST",
                                    key + " : " + this.toJson().getString(key)
                            )
                    );
                }
            }else if(this.toJson().get(key) instanceof ArrayList<?>){
                if(!this.toJson().get(key).toString().equals(old.toJson().get(key).toString())){

                }
            }
        }
        return changes;
    }

    public void addTag(String tag) {
        this.tags.add(tag.toLowerCase().trim());
    }

    public void setPriority(Double priority) {
        this.priority = priority;
    }

    public static class MarkdownEscapeUtils {
        public static String escapeMarkdownForJson(String markdownText) {
            String escapedText = markdownText.replace("\\", "\\\\");
            escapedText = escapedText.replace("\"", "\\\"");
            escapedText = escapedText.replace("\n", "\\n");
            escapedText = escapedText.replace("\r", "\\r");
            escapedText = escapedText.replace("\t", "\\t");
            escapedText = escapedText.replace("\b", "\\b");
            escapedText = escapedText.replace("\f", "\\f");
            return escapedText;
        }
    }
}