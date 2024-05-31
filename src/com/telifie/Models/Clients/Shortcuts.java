package com.telifie.Models.Clients;

import com.telifie.Models.Shortcut;
import com.telifie.Models.Utilities.Network.SQL;
import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Telifie;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Shortcuts extends Client {

    public Shortcuts(Session session){
        super(session);
        super.collection = "shortcuts";
    }

    public ArrayList<Shortcut> getShortcuts(){
        ArrayList<Shortcut> shortcuts = new ArrayList<>();
        try{
            ResultSet ss = SQL.get("SELECT * FROM shortcuts WHERE user = ?", session.user);
            while (ss.next()) {
                Shortcut s = new Shortcut(ss);
                shortcuts.add(s);
            }
            return shortcuts;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean save(Shortcut shortcut){
        return SQL.update("INSERT INTO shortcuts (user, object, name, icon, origin) VALUES (?, ?, ?, ?, ?)", session.user, shortcut.object, shortcut.name, shortcut.icon, Telifie.epochTime());
    }

    public boolean unsave(String objId){
        return SQL.update("DELETE FROM shortcuts WHERE user = ? AND object = ? LIMIT 1", session.user, objId);
    }
}