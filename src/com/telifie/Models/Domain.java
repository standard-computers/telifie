package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class Domain {

    public final String id, owner, name, alt;
    public final int origin, permissions;
    private ArrayList<Member> users = new ArrayList<>();
    private ArrayList<Collection> collections = new ArrayList<>();

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
        this.users = new ArrayList<>();
        this.collections = new ArrayList<>();
    }

    public int getPermissions(String userId){
        if(this.owner.equals(userId)){
            return 0;
        }else{
            for(Member u : users){
                if(u.user.equals(userId)){
                    int up = u.permissions;
                    return (up < 1 || up > 2 ? 1 : u.permissions);
                }
            }
        }
        return -1;
    }

    public ArrayList<Member> getUsers() {
        return users;
    }

    public ArrayList<Collection> getCollections() {
        return collections;
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
                ", \"collections\" :" + collections +
                '}';
    }

    public static class Member {

        public final String user;
        public final int permissions;

        public Member(ResultSet rs) throws SQLException {
            this.user = rs.getString("user");
            this.permissions = rs.getInt("permissions");
        }

        @Override
        public String toString(){
            return new StringBuilder().append("{\"user\" : \"").append(user).append("\", \"permissions\" : \"").append(permissions).append("\"}").toString();
        }
    }
}