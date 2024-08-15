package com.telifie.Models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Domain {

    public final String id, owner, name;
    public final int permissions;
    private final ArrayList<Member> users;
    private final ArrayList<Index> indexes;

    public Domain(ResultSet rs) throws SQLException {
        this.id = rs.getString("id");
        this.owner = rs.getString("owner");
        this.name = rs.getString("name");
        this.permissions = rs.getInt("permissions");
        this.users = new ArrayList<>();
        this.indexes = new ArrayList<>();
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
        return "{\"id\" : \"" + id + '\"' +
                ", \"owner\" : \"" + owner + '\"' +
                ", \"name\" : \"" + name + '\"' +
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