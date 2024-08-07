package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Log;
import com.telifie.Models.Utilities.Service;
import java.util.ArrayList;

public class Services {

    private static ArrayList<Service> services = new ArrayList<>();

    public Services(){
        Log.console("LOADING SERVICES...");
        services.removeAll(services);
        Log.console(services.size() + " SERVICES LOADED");
    }
}