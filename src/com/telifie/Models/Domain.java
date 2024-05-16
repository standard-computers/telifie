package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class Domain {

    public final String id, owner, name, alias;
    public final int origin, permissions;
    private ArrayList<Member> users = new ArrayList<>();
    private ArrayList<Index> indexes = new ArrayList<>();

    public Domain(String owner, String name, int permissions){
        this.id = UUID.randomUUID().toString();
        this.owner = owner;
        this.name = name;
        this.alias = name.toLowerCase().replaceAll(" ", "-");
        this.origin = Telifie.epochTime();
        this.permissions = (permissions != 1 ? 0 : 1); //Private is default mode if none
    }

    public Domain(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getString("id");
        this.owner = resultSet.getString("owner");
        this.name = resultSet.getString("name");
        this.alias = resultSet.getString("alias");
        this.origin = resultSet.getInt("origin");
        this.permissions = resultSet.getInt("permissions");
        this.users = new ArrayList<>();
        this.indexes = new ArrayList<>();
    }

    public String getId() {
        return id;
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

    public boolean hasEditPermissions(String userId){
        if(userId.equals(this.owner)){
            return true;
        }else{
            for(Member u : users){
                if(u.user.equals(userId)){
                    return  u.permissions == 1;
                }
            }
        }
        return false;
    }

    public boolean hasViewPermissions(String userId){
        if(permissions == 1){
            return true;
        }else{
            for(Member u : users){
                if(u.user.equals(userId)){
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Member> getUsers() {
        return users;
    }

    public ArrayList<Index> getIndexes() {
        return indexes;
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\" : \"" + id + '\"' +
                ", \"owner\" : \"" + owner + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"alias\" : \"" + alias + '\"' +
                ", \"origin\" :" + origin +
                ", \"permissions\" :" + permissions +
                ", \"users\" :" + users +
                ", \"indexes\" :" + indexes +
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