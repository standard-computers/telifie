package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.util.ArrayList;
import java.util.UUID;

public class Collection {

    private String id, domain, user, icon, name, sort = "A", connector = "";
    private int origin, permissions = Telifie.PRIVATE;
    private ArrayList articles;
    private ArrayList<Article> detailedList;

    public Collection(String name){
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.origin = Telifie.getEpochTime();
    }

    public Collection(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? Telifie.tools.make.md5(Telifie.tools.make.randomReferenceCode()) : document.getString("id") );
        this.domain = document.getString("domain");
        this.user = document.getString("user");
        this.icon = document.getString("icon");
        this.name = document.getString("name");
        this.connector = document.getString("connector");
        this.articles = document.get("articles", ArrayList.class);
        this.origin = Telifie.getEpochTime();
        this.permissions = document.getInteger("permissions");
    }

    public String getId() {
        return id;
    }

    public String getDomain() {
        return domain;
    }

    public Collection setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPermissions() {
        return permissions;
    }

    public ArrayList<String> getArticles() {
        return articles;
    }

    public void setArticles(ArrayList articles) {
        this.articles = articles;
    }

    public void setDetailedList(ArrayList<Article> detailedList) {
        this.detailedList = detailedList;
    }

    @Override
    public String toString() {

        String articles = "[]";
        if(this.articles != null){

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < this.articles.size(); i++) {
                sb.append("\"");
                sb.append(this.articles.get(i));
                sb.append("\"");
                if (i < this.articles.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            articles = sb.toString();

        }

        return "{" +
                "\"id\" : \"" + id + '\"' +
                (this.domain == null ? "" : ", \"domain\" : \"" + domain + '\"') +
                ", \"user\" : \"" + user + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"sort\" : \"" + sort + '\"' +
                ", \"connector\" : \"" + connector + '\"' +
                ", \"origin\" : \"" + origin + '\"' +
                ", \"permissions\" : " + permissions +
                (this.detailedList == null ? (this.articles != null ? ", \"articles\" : " + articles : ", \"articles\" : []") : ", \"articles\" : " + detailedList ) +
                '}';
    }
}
