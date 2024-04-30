package com.telifie.Models.Clients;

import com.telifie.Models.Collection;
import com.telifie.Models.Utilities.Network.SQL;

public class Collections {

    public static void create(Collection c){
        SQL.update("INSERT INTO collections (id, domain, name, alt, origin) VALUES (?, ?, ?, ?, ?)", c.id, c.domain, c.name, c.alt, c.origin);
    }
}
