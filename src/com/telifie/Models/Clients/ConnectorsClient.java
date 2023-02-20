package com.telifie.Models.Clients;

import com.telifie.Models.Domain;

public class ConnectorsClient extends Client {

    public ConnectorsClient(String mongoUri) {
        super(mongoUri);
    }

    public ConnectorsClient(Domain domain) {
        super(domain);
    }

}
