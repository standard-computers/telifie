package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.util.ArrayList;
import java.util.UUID;

public class Collection {

    private String id, user, icon, name, sort = "A";
    private int origin, permissions = Telifie.PRIVATE;
    private ArrayList articles;
    private ArrayList<Article> detailedList;

    public Collection(String user, String name){
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.name = name;
        this.origin = Telifie.getEpochTime();
    }

    public Collection(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? Telifie.tools.make.md5(Telifie.tools.make.randomReferenceCode()) : document.getString("id") );
        this.user = document.getString("user");
        this.icon = document.getString("icon");
        this.name = document.getString("name");
        this.articles = document.get("articles", ArrayList.class);
        this.origin = Telifie.getEpochTime();
        this.permissions = document.getInteger("permissions");
    }

    public String getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
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
                ", \"user\" : \"" + user + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"origin\" : \"" + origin + '\"' +
                ", \"permissions\" : " + permissions +
                (this.detailedList == null ? (this.articles != null ? ", \"articles\" : " + articles : ", \"articles\" : []") : ", \"articles\" : " + detailedList ) +
                '}';
    }
}
