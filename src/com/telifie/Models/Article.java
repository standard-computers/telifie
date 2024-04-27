package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

public class Article {

    private String owner, id, title, link, icon, description = "Webpage", content, source;
    private boolean verified = false;
    private ArrayList<String> tags = new ArrayList<>();
    private ArrayList<Attribute> attributes = new ArrayList<>();
    private ArrayList<Association> associations = new ArrayList<>();
    private ArrayList<DataSet> dataSets = new ArrayList<>();
    private int priority = 1;
    private final int origin;

    public Article(){
        this.id = UUID.randomUUID().toString();
        this.origin = Telifie.epochTime();
    }

    public Article(Document document) throws NullPointerException {
        this.owner = (document.getString("owner") == null ? null : document.getString("owner"));
        this.id = (document.getString("id") == null ? UUID.randomUUID().toString() : document.getString("id"));
        this.verified = (document.getBoolean("verified") != null && document.getBoolean("verified"));
        this.title = Telifie.tools.escape(document.getString("title"));
        this.link = document.getString("link");
        this.icon = document.getString("icon");
        this.description = document.getString("description");
        this.source = document.getString("source");
        this.content = (document.getString("content") != null ?  Telifie.tools.escape(document.getString("content")) : "");
        this.origin = (document.getInteger("origin") == null ? 0 : document.getInteger("origin"));
        this.priority = (document.getInteger("priority") == null ? 0 : document.getInteger("priority"));
        this.tags = document.get("tags", ArrayList.class);
        ArrayList<Document> it2 = (ArrayList<Document>) document.getList("attributes", Document.class);
        if (it2 != null) {
            it2.forEach(doc -> this.addAttribute(new Attribute(doc.getString("key"), doc.getString("value"))));
        }
        ArrayList<Document> it3 = (ArrayList<Document>) document.getList("associations", Document.class);
        if (it3 != null) {
            it3.forEach(doc -> this.associations.add(new Association(doc)));
        }
        ArrayList<Document> it4 = (ArrayList<Document>) document.getList("data_sets", Document.class);
        if(it4 != null){
            it4.forEach(doc -> this.dataSets.add(new DataSet(doc)));
        }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isVerified() {
        return this.verified;
    }

    public String getId() {
        return id;
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

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
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

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                (owner == null ? "" : ", \"owner\" : \"" + owner + "\"") +
                ", \"verified\" : " + verified +
                ", \"title\" : \"" + title + '\"' +
                (link == null ? "" : ", \"link\" : \"" + link + '\"') +
                (icon == null ? "" : ", \"icon\" : \"" + icon + '\"') +
                (description == null || description.isEmpty() ? "" : ", \"description\" : \"" + description + '\"') +
                (content == null ? "" : ", \"content\" : \"" + content + "\"") +
                (tags == null ? "" : ", \"tags\" : " + tags.stream().map(tag -> "\"" + tag + "\"").collect(Collectors.joining(", ", "[", "]"))) +
                (attributes == null ? "" : ", \"attributes\" : " + attributes) +
                ", \"associations\" : " + (associations ==  null || associations.isEmpty() ? "[]" : associations) +
                (dataSets.isEmpty() ? "" : ", \"data_sets\" : " + dataSets) +
                (source == null ? "" : ", \"source\" : \"" + source + '\"') +
                ", \"origin\" : " + origin +
                ", \"priority\" : " + priority +
                '}';
    }

    public int getPriority() {
        return this.priority;
    }

    public class DataSet {

        private final String title;
        private final ArrayList<String[]> rows;

        public DataSet(Document document){
            this.title = document.getString("title");
            this.rows = (ArrayList<String[]>) document.get("rows", ArrayList.class);
        }

        @Override
        public String toString() {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{ \"title\": \"").append(title).append("\", \"rows\": [");
            for (String[] row : rows) {
                jsonBuilder.append("[\"").append(String.join("\", \"", row)).append("\"],");
            }
            if (!rows.isEmpty()) {
                jsonBuilder.deleteCharAt(jsonBuilder.length() - 1); // Remove the trailing comma
            }
            jsonBuilder.append("] }");
            return jsonBuilder.toString();
        }
    }

    public static class Source {

        public final String icon;
        public final String name;
        public final String url;

        public Source(String icon, String name, String url) {
            this.icon = icon;
            this.name = name;
            this.url = url;
        }

        public Source(Document document) throws NullPointerException {
            this.icon = document.getString("icon");
            this.name = document.getString("name");
            this.url = document.getString("url");
        }

        @Override
        public String toString() {
            return new StringBuilder().append("{\"icon\" : \"").append(icon).append('\"').append(", \"name\" : \"").append(name).append('\"').append(", \"url\" : \"").append(url).append("\"}").toString();
        }
    }
}