package com.telifie.Models.Clients;

import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Network.SQL;
import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Telifie;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Domains {

    private Session session;

    public Domains(Session session){
        this.session = session;
    }

    public ArrayList<Domain> mine(){
        ArrayList<Domain> domainList = new ArrayList<>();
        try {
            ResultSet m = SQL.get("SELECT * FROM domains WHERE owner = ?", session.user);
            while (m.next()) {
                Domain domain = new Domain(m);
                domainList.add(domain);
            }
        } catch (SQLException e) {
            e.printStackTrace(); //Print the stack trace for detailed error information
            return null;
        }
        return domainList;
    }

    public boolean delete(Domain d){
        return SQL.delete("DELETE FROM domains WHERE id = ? AND owner = ?", d.id, session.user);
    }

    public boolean create(Domain d){
        return SQL.update("INSERT INTO domains (id, owner, name, alt, permissions, origin) VALUES (?, ?, ?, ?, ?, ?)", d.id, d.owner, d.name, d.alt, d.permissions, Telifie.epochTime());
    }

    public boolean addUser(Domain d, String userId, int permissions){
        return SQL.update("INSERT INTO memberships (user, domain, origin, permissions) VALUES (?, ?, ?, ?)", userId, d.id, Telifie.epochTime(), permissions);
    }

    public boolean removeUser(Domain d, String userId){
        return SQL.delete("DELETE FROM memberships WHERE user = ? AND domain = ?", userId, d.id);
    }

    public boolean updateUser(Domain d, String userId, int permissions){
        return SQL.update("UPDATE memberships SET permissions = ? WHERE domain = ? AND user = ?", permissions, d.id, userId);
    }

    public Domain withId(String id){
        try {
            ResultSet resultSet = SQL.get("SELECT * FROM domains WHERE id = ? LIMIT 1", id);
            if (resultSet.next()) {
                return new Domain(resultSet);
            } else return null;
        } catch (SQLException e) {
            return null;
        }
    }
}