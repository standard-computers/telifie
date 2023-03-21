package com.telifie.Models;

import com.telifie.Models.Actions.Event;
import com.telifie.Models.Articles.Association;
import com.telifie.Models.Articles.Attribute;
import com.telifie.Models.Articles.Image;
import com.telifie.Models.Articles.Source;
import com.telifie.Models.Utilities.Tool;
import org.bson.Document;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class Article implements Serializable {

    private String id, title, link, icon, description;
    private boolean verified = false;
    private String content;
    private ArrayList<String> tags;
    private ArrayList<Image> images = new ArrayList<>();
    private ArrayList<Attribute> attributes = new ArrayList<>();
    private ArrayList<Association> associations = new ArrayList<>();
    private Source source;
    private int origin;

    public Article(){

        this.id = Tool.md5(Tool.eid());
        this.title = title;
        this.link = link;
        this.origin = (int) (System.currentTimeMillis() / 1000);
    }

    public Article(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? Tool.md5(Tool.eid()) : document.getString("id"));
        this.verified = (document.getBoolean("verified") == null ? false : document.getBoolean("verified"));
        this.title = document.getString("title");
        this.link = document.getString("link");
        this.icon = document.getString("icon");
        this.description = document.getString("description");

        String contentString = (document.getString("content") != null ? document.getString("content").replaceAll("\\s+|\\r?\\n", " ") : "");
        this.content = contentString.replaceAll("\"", "&quot;");
        this.origin = (document.getInteger("origin") == null ? 0 : document.getInteger("origin"));
        this.tags = document.get("tags", ArrayList.class);

        ArrayList<Document> iterable = (ArrayList<Document>) document.getList("images", Document.class);
        if (iterable != null && iterable.size() >= 1) {

            for (Document doc : iterable) {

                this.addImage(new Image(doc));
            }
        }

        ArrayList<Document> iterable2 = (ArrayList<Document>) document.getList("attributes", Document.class);
        if (iterable2 != null) {

            for (Document doc : iterable2) {

                this.addAttribute(new Attribute(doc.getString("key"), doc.getString("value")));
            }
        }

        ArrayList<Document> iterable3 = (ArrayList<Document>) document.getList("associations", Document.class);
        if (iterable3 != null) {

            for (Document doc : iterable3) {

                this.addAssociation(new Association(doc));
            }
        }

        Document sourceDocument = document.get("source", Document.class);
        if (sourceDocument != null) {

            this.source = new Source(sourceDocument);
        }
    }

    public Article(String title, String link, String icon, String description) {

        this.id = Tool.eid();
        this.title = title;
        this.link = link;
        this.icon = icon;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
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

    public String getIcon() {
        return icon;
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

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public ArrayList<Image> getImages() {
        return images;
    }

    public void setImages(ArrayList<Image> images) {
        this.images = images;
    }

    public void addImage(Image image){
        this.images.add(image);
    }

    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<Attribute> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(Attribute attr){
        this.attributes.add(attr);
    }

    public ArrayList<Association> getAssociations() {
        return associations;
    }

    public void setAssociations(ArrayList<Association> associations) {
        this.associations = associations;
    }

    public void addAssociation(Association ass){
        this.associations.add(ass);
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    @Override
    public String toString() {
        String tags = "[]";

        if(this.tags != null){

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < this.tags.size(); i++) {
                sb.append("\"");
                sb.append(this.tags.get(i));
                sb.append("\"");
                if (i < this.tags.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            tags = sb.toString();

        }

        return "{\"id\" : \"" + id + '\"' +
                ", \"verified\" : " + verified +
                ", \"title\" : \"" + title + '\"' +
                (link == null ? "" : ", \"link\" : \"" + link + '\"') +
                (icon == null ? "" : ", \"icon\" : \"" + icon + '\"') +
                (description == null || description.equals("") ? "" : ", \"description\" : \"" + description + '\"') +
                (content == null ? "" : ", \"content\" : \"" + content + "\"") +
                ", \"tags\" : " + tags +
                (images.equals("null") ? "" : ", \"images\" : " + images) +
//                ", \"images\" : " + images +
                ", \"attributes\" : " + attributes +
                ", \"associations\" : " + associations +
                ", \"source\" : " + source +
                ", \"origin\" : " + origin +
                '}';
    }

    public JSONObject toJson(){
        JSONObject json = new JSONObject(this.toString());
        return json;
    }

    public ArrayList<Event> compare(Article old){

        ArrayList<Event> changes = new ArrayList<>();

        Iterator<String> newKeys = this.toJson().keys();
        while (newKeys.hasNext()) {

            String key = newKeys.next();
            if(this.toJson().get(key) instanceof String){

                if(old.toJson().has(key) && !this.toJson().getString(key.toString()).equals(old.toJson().getString(key.toString()))){ //If the value at key HAS changed

                    //i.e. {"title", "old_title_value", "new_title_value"}
                    changes.add(
                            new Event(
                                    Event.Type.UPDATE,
                                    Tool.epochTime(),
                                    "GUEST",
                                    key + " : " + old.toJson().getString(key) + " => " + key + " : " + this.toJson().getString(key)
                            )
                    );
                }else if(!old.toJson().has(key)){

                    changes.add(
                            new Event(
                                    Event.Type.PUT,
                                    Tool.epochTime(),
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
}
