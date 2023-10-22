package com.telifie.Models;

import com.telifie.Models.Articles.*;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

public class Article {

    private String owner, domain, id, title, link, icon, description;
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
        this.origin = Telifie.epochTime();
    }

    public Article(Document document) throws NullPointerException {
        this.owner = (document.getString("owner") == null ? null : document.getString("owner"));
        this.domain = (document.getString("domain") == null ? null : document.getString("domain"));
        this.id = (document.getString("id") == null ? UUID.randomUUID().toString() : document.getString("id"));
        this.verified = (document.getBoolean("verified") != null && document.getBoolean("verified"));
        this.title = Andromeda.tools.escape(document.getString("title"));
        this.link = document.getString("link");
        this.icon = document.getString("icon");
        this.description = document.getString("description");
        this.priority = (document.getDouble("priority") == null ? 1.01 : document.getDouble("priority"));
        this.content = (document.getString("content") != null ?  Andromeda.tools.escapeMarkdownForJson(document.getString("content")) : "");
        this.origin = (document.getInteger("origin") == null ? 0 : document.getInteger("origin"));
        this.tags = document.get("tags", ArrayList.class);

        ArrayList<Document> iterable = (ArrayList<Document>) document.getList("images", Document.class);
        if (iterable != null) {
            iterable.forEach(doc -> this.addImage(new Image(doc)));
        }
        ArrayList<Document> it2 = (ArrayList<Document>) document.getList("attributes", Document.class);
        if (it2 != null) {
            it2.forEach(doc -> this.addAttribute(new Attribute(doc.getString("key"), doc.getString("value"))));
        }
        ArrayList<Document> it3 = (ArrayList<Document>) document.getList("associations", Document.class);
        if (it3 != null) {
            it3.forEach(doc -> this.addAssociation(new Association(doc)));
        }
        ArrayList<Document> it4 = (ArrayList<Document>) document.getList("data_sets", Document.class);
        if(it4 != null){
            it4.forEach(doc -> this.addDataSet(new DataSet(doc)));
        }
        Document sourceDocument = document.get("source", Document.class);
        if (sourceDocument != null) {
            this.source = new Source(sourceDocument);
        }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public boolean isVerified() {
        return this.verified;
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

    public double getPriority() {
        return priority;
    }

    public void setPriority(Double priority) {
        this.priority = priority;
    }

    public void addTag(String tag) {
        this.tags.add(tag.toLowerCase().trim());
    }

    public void addTags(String[] tags){
        for(String tag : tags){
            this.tags.add(tag.toLowerCase().trim());
        }
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

    public ArrayList<Image> getImages() {
        return images;
    }

    public void addImage(Image image){
        this.images.add(image);
    }

    public void addAttribute(Attribute attr){
        if (!attributeExists(attr)) {
            this.attributes.add(attr);
        }
    }

    private boolean attributeExists(Attribute attr) {
        for (Attribute existingAttr : this.attributes) {
            if (existingAttr.key().equals(attr.key()) && existingAttr.value().equals(attr.value())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAttribute(String key){
        for(Attribute attr : this.attributes){
            if(attr.key().toLowerCase().trim().equals(key)){
                return true;
            }
        }
        return false;
    }

    public String getAttribute(String key){
        for(Attribute attr : this.attributes){
            if(attr.key().toLowerCase().trim().equals(key.toLowerCase())){
                return attr.value();
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

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                (owner == null ? "" : ", \"owner\" : \"" + owner + "\"") +
                (domain == null ? "" : ", \"domain\" : \"" + domain + "\"") +
                ", \"verified\" : " + verified +
                ", \"title\" : \"" + title + '\"' +
                (link == null ? "" : ", \"link\" : \"" + link + '\"') +
                (icon == null ? "" : ", \"icon\" : \"" + icon + '\"') +
                (description == null || description.isEmpty() ? "" : ", \"description\" : \"" + description + '\"') +
                (priority == 0 ? "" : ", \"priority\" : " + priority) +
                (content == null ? "" : ", \"content\" : \"" + content + "\"") +
                (tags == null ? "" : ", \"tags\" : " + tags.stream().map(tag -> "\"" + tag + "\"").collect(Collectors.joining(", ", "[", "]"))) +
                (images == null ? "" : ", \"images\" : " + images) +
                (attributes == null ? "" : ", \"attributes\" : " + attributes) +
                (associations.isEmpty() ? "" : ", \"associations\" : " + associations) +
                (dataSets.isEmpty() ? "" : ", \"data_sets\" : " + dataSets) +
                (source == null ? ", \"source\" : null" : ", \"source\" : " + source) +
                ", \"origin\" : " + origin +
                '}';
    }
}