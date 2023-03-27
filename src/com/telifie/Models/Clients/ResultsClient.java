package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Configuration;

public class ResultsClient extends Client{

    //constructor that makes a new client and sets collection to results
    public ResultsClient(Configuration config){
        super(config);
        this.collection = "results";
    }
}
