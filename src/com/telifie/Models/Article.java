package com.telifie.Models;

import com.telifie.Models.Actions.Event;
import com.telifie.Models.Articles.*;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class Article {

    private String id, title, link, icon, description;
    private double priority = 1.01;
    private boolean verified = false;
    private String content;
    private ArrayList<String> tags = new ArrayList<>();
    private ArrayList<Image> images = new ArrayList<>();
    private ArrayList<Attribute> attributes = new ArrayList<>();
    private ArrayList<Association> associations = new ArrayList<>();
    private ArrayList<DataSet> dataSets = new ArrayList<DataSet>();
    private Source source;
    private int origin;

    public Article(){
        this.id = UUID.randomUUID().toString();
        this.origin = (int) (System.currentTimeMillis() / 1000);
    }

    public Article(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? UUID.randomUUID().toString() : document.getString("id"));
        this.verified = (document.getBoolean("verified") == null ? false : document.getBoolean("verified"));
        this.title = document.getString("title");
        this.link = document.getString("link");
        this.icon = document.getString("icon");
        this.description = document.getString("description");
        this.priority = (document.getDouble("priority") == null ? 1.01 : document.getDouble("priority"));
        String contentString = (document.getString("content") != null ? document.getString("content").replaceAll("\\s+|\\r?\\n", " ") : "");
        this.content = contentString.replaceAll("\"", "&quot;");
        this.origin = (document.getInteger("origin") == null ? 0 : document.getInteger("origin"));
        this.tags = document.get("tags", ArrayList.class);

        ArrayList<Document> iterable = (ArrayList<Document>) document.getList("images", Document.class);
        if (iterable != null && iterable.size() >= 1) {
            for (Document doc : iterable) {
                Image ni = new Image(doc);
                ni.setId(this.getId());
                this.addImage(new Image(doc));
            }
        }
        ArrayList<Document> it2 = (ArrayList<Document>) document.getList("attributes", Document.class);
        if (it2 != null) {
            for (Document doc : it2) {
                this.addAttribute(new Attribute(doc.getString("key"), doc.getString("value")));
            }
        }
        ArrayList<Document> it3 = (ArrayList<Document>) document.getList("associations", Document.class);
        if (it3 != null) {
            for (Document doc : it3) {
                this.addAssociation(new Association(doc));
            }
        }
        ArrayList<Document> it4 = (ArrayList<Document>) document.getList("data_sets", Document.class);
        if(it4 != null){
            for(Document doc : it4){
                this.addDataSet(new DataSet(doc));
            }
        }
        Document sourceDocument = document.get("source", Document.class);
        if (sourceDocument != null) {
            this.source = new Source(sourceDocument);
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
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
                (priority == 0 ? "" : ", \"priority\" : " + priority) +
                (content == null ? "" : ", \"content\" : \"" + content + "\"") +
                (this.tags == null || this.tags.size() == 0 ? "" : ", \"tags\" : " + tags) +
                (images.equals("null") || images == null || images.size() == 0 ? "" : ", \"images\" : " + images) +
                (attributes.size() == 0 ? "" : ", \"attributes\" : " + attributes) +
                (associations.size() == 0 ? "" : ", \"associations\" : " + associations) +
                (dataSets.size() == 0 ? "" : ", \"data_sets\" : " + dataSets) +
                (source == null ? "" : ", \"source\" : " + source) +
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
}
