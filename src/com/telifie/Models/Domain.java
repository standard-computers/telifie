package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class Domain implements Serializable {

    private final String id, owner, name, alt;
    private final int origin, permissions;
    private final ArrayList<Member> users = new ArrayList<>();

    public Domain(String owner, String name, int permissions){
        this.owner = owner;
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.alt = name.toLowerCase().replaceAll(" ", "-");
        this.origin = Telifie.epochTime();
        this.permissions = (permissions != 1 ? 0 : 1); //Private is default mode if none
    }

    public Domain(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? UUID.randomUUID().toString() : document.getString("id"));
        this.owner = document.getString("owner");
        this.name = document.getString("name");
        this.alt = document.getString("alt");
        this.permissions = (document.getInteger("permissions") == null ? 0 : document.getInteger("permissions"));
        this.origin = (document.getInteger("origin") == null ? 0 : document.getInteger("origin"));
        ArrayList<Document> m = (ArrayList<Document>) document.getList("users", Document.class);
        if (m != null) {
            m.forEach(d -> this.addUser(new Member(d)));
        }
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
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
                if(u.id.equals(userId)){
                    int up = u.permissions;
                    return (up < 1 || up > 2 ? 1 : u.permissions);
                }
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\" : \"" + id + '\"' +
                ", \"owner\" : \"" + owner + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"alt\" : \"" + alt + '\"' +
                ", \"origin\" :" + origin +
                ", \"permissions\" :" + permissions +
                ", \"users\" :" + users +
                '}';
    }

    public static class Member {

        public final String id;
        public final int permissions;

        public Member(Document doc){
            this.id = doc.getString("id");
            this.permissions = (doc.getInteger("permissions") == null ? 0 : doc.getInteger("permissions"));
        }

        @Override
        public String toString(){
            return new StringBuilder().append("{\"id\" : \"").append(id).append("\", \"permissions\" : \"").append(permissions).append("\"}").toString();
        }
    }
}