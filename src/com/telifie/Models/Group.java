package com.telifie.Models;

import com.telifie.Models.Utilities.Tool;
import com.telifie.Models.Utilities.Vars;
import org.bson.Document;
import java.util.ArrayList;

public class Group {

    private String id, user, icon, name;
    private int origin, permissions = Vars.PRIVATE;
    private ArrayList articles;
    private ArrayList<Article> detailedList;

    public Group(String user, String name){
        this.id = Tool.md5(Tool.eid());
        this.user = user;
        this.name = name;
        this.origin = Tool.epochTime();
    }

    public Group(String user, String icon, String name) {
        this.id = Tool.md5(Tool.eid());
        this.user = user;
        this.icon = icon;
        this.name = name;
        this.origin = Tool.epochTime();
    }

    public Group(Document document){
        if(document != null){
            this.id = (document.getString("id") == null ? Tool.md5(Tool.eid()) : document.getString("id") );
            this.user = document.getString("user");
            this.icon = document.getString("icon");
            this.name = document.getString("name");
            this.articles = document.get("articles", ArrayList.class);
            this.origin = Tool.epochTime();
            this.permissions = document.getInteger("permissions");
        }
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

    public String getIcon() {
        return icon;
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

    public int getOrigin() {
        return origin;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public ArrayList<String> getArticles() {
        return articles;
    }

    public void setArticles(ArrayList articles) {
        this.articles = articles;
    }

    public ArrayList<Article> getDetailedList() {
        return detailedList;
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
