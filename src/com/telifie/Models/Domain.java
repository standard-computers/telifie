package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class Domain implements Serializable {

    public final String id, owner, name, alt;
    public final int origin, permissions;
    private ArrayList<Member> users = new ArrayList<>();

    public Domain(String owner, String name, int permissions){
        this.id = UUID.randomUUID().toString();
        this.owner = owner;
        this.name = name;
        this.alt = name.toLowerCase().replaceAll(" ", "-");
        this.origin = Telifie.epochTime();
        this.permissions = (permissions != 1 ? 0 : 1); //Private is default mode if none
    }

    public Domain(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getString("id");
        this.owner = resultSet.getString("owner");
        this.name = resultSet.getString("name");
        this.alt = resultSet.getString("alt");
        this.origin = resultSet.getInt("origin");
        this.permissions = resultSet.getInt("permissions");
//        this.users = (ArrayList<Member>) resultSet.getArray("users");
    }

    public int getPermissions(String userId){
        if(this.owner.equals(userId)){
            return 0;
        }else{
            for(Member u : users){
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