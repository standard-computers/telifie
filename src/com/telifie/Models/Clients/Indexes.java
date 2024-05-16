package com.telifie.Models.Clients;

import com.telifie.Models.Index;
import com.telifie.Models.Utilities.Network.SQL;

public class Indexes {

    public static boolean create(Index c){
        return SQL.update("INSERT INTO indexes (id, domain, name, alias, origin) VALUES (?, ?, ?, ?, ?)", c.id, c.domain, c.name, c.alias, c.origin);
    }
}