package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class Domain implements Serializable {

    private String id, icon, owner, name;
    private int origin, permissions;
    private final ArrayList<Member> users = new ArrayList<>();

    public Domain(String owner, String name, String icon, int permissions){
        this.owner = owner;
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.icon = icon;
        this.origin = Telifie.epochTime();
        this.permissions = (permissions <= 2 && permissions >= 0 ? permissions : 0); //Private is default mode if none
    }

    public Domain(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? UUID.randomUUID().toString() : document.getString("id"));
        this.icon = document.getString("icon");
        this.owner = document.getString("owner");
        this.name = document.getString("name");
        this.permissions = (document.getInteger("permissions") == null ? 0 : document.getInteger("permissions"));
        ArrayList<Document> m = (ArrayList<Document>) document.getList("users", Document.class);
        if (m != null) {
            m.forEach(d -> this.addUser(new Member(d)));
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public void addUser(Member member){
        this.users.add(member);
    }

    public boolean hasPermission(String userId){
        //TODO Change using code from getPermissions
        return this.owner.equals(userId);
    }

    public int getPermissions(String userId){
        if(this.owner.equals(userId)){
            return 0;
        }else{
            for(Member u : users){
                //TODO expects email, userId given
                if(u.getId().equals(userId)){
                    int up = u.permissions();
                    return (up < 1 || up > 2 ? 1 : u.permissions());
                }
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\" : \"" + id + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"owner\" : \"" + owner + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"origin\" :" + origin +
                ", \"permissions\" :" + permissions +
                ", \"users\" :" + users +
                '}';
    }
}