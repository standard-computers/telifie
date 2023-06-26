package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class Domain implements Serializable {

    private String uri, id, alt = "telifie", icon, owner, name;
    private int origin, permissions;
    private final ArrayList<Member> users = new ArrayList<>();

    public Domain(String uri){
        this.uri = uri;
    }

    public Domain(String owner, String name, String icon, int permissions){
        this.owner = owner;
        this.id = UUID.randomUUID().toString();
        this.alt = Telifie.tools.make.randomReferenceCode();
        this.name = name;
        this.icon = icon;
        this.origin = Telifie.getEpochTime();
        this.permissions = (permissions <= 2 && permissions >= 0 ? permissions : 0); //Private is default mode if none
    }

    public Domain(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? UUID.randomUUID().toString() : document.getString("id"));
        this.alt = document.getString("alt");
        this.icon = document.getString("icon");
        this.owner = document.getString("owner");
        this.name = document.getString("name");
        this.permissions = (document.getInteger("permissions") == null ? 0 : document.getInteger("permissions"));
        ArrayList<Document> m = (ArrayList<Document>) document.getList("users", Document.class);
        if (m != null) {
            m.forEach(d -> this.addUser(new Member(d)));
        }
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri){
        this.uri = uri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public Domain setName(String name) {
        this.name = name;
        return this;
    }

    public ArrayList<Member> getUsers() {
        return users;
    }

    public void addUser(Member member){
        this.users.add(member);
    }

    @Override
    public String toString() {
        return "{" +
                (uri == null ? "" : "\"uri\" : \"" + uri + "\", ") +
                "\"id\" : \"" + id + '\"' +
                ", \"alt\" : \"" + alt + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"owner\" : \"" + owner + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"origin\" :" + origin +
                ", \"permissions\" :" + permissions +
                ", \"users\" :" + users +
                '}';
    }
}
