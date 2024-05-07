package com.telifie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.Articles;
import com.telifie.Models.Clients.Cache;
import com.telifie.Models.Clients.Packages;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Network.Http;
import org.bson.Document;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Start {

    private static Configuration config;
    private static final File configFile = new File(Telifie.configDirectory() + "/config.json");

    public static void main(String[] args){
        Log.message("TELIFIE STARTED", "CLIx001");
        checkConfig();
        Console.welcome();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.flag("TELIFIE EXITED", "CLIx101");
            Telifie.purgeTemp();
        }));
        Articles articles = new Articles(new Session("telifie", "telifie"));
        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            Log.console("Preloading public domain stats in background...");
            CompletableFuture.runAsync(() -> Telifie.stats = articles.stats());
            Log.console("Scheduling worker tasks...");
            Runnable task = () -> Telifie.stats = articles.stats();
            scheduler.scheduleAtFixedRate(task, 0, 90, TimeUnit.SECONDS);
        }
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            switch (mode) {
                case "--http" -> {
                    try {
                        Log.console("Starting HTTP server [CONSIDER HTTPS FOR SECURITY]...");
                        new Http();
                    } catch (Exception e) {
                        Log.error("HTTP SERVER FAILED", "CLIx102");
                    }
                }
                case "--https" -> {
                    try {
                        Log.console("Starting HTTPS server...");
                        new Http();
                    } catch (Exception e) {
                        Log.error("HTTPS SERVER FAILED", "CLIx103");
                    }
                }
                case "--master" -> CompletableFuture.runAsync(() -> {
                    if(Cache.history.isEmpty()){
                        Log.console("Quick Response cache is empty. Warming up....\nFinding cache qualifying articles. This may take some time!");
                        ArrayList<Article> av = articles.get(new Document("priority", new Document("$gt", 1)));
                        Log.console("Committing " + av.size() + " to quick response cache...");
//                        av.forEach(a -> Cache.history.commit("telifie", a.getId(), a.getTitle(), a.getIcon(), a.getDescription()));
                    }
                });
            }
        }else{
            Console.command();
        }
    }

    private static void checkConfig(){
        File[] folders = new File[]{new File(Telifie.configDirectory()), new File(Telifie.configDirectory() + "temp"), new File(Telifie.configDirectory() + "andromeda")};
        for(File folder : folders){
            if(folder.mkdirs()){
                Log.put("CREATED DIRECTORY : " + folder.getPath(), "CLIx004");
            }else{
                Log.error("FAILED CREATING DIRECTORY : " + folder.getPath(), "CLIx104");
            }
        }
        if(configFile.exists()){
            Console.message("Config file found :)");
            importConfiguration();
            if (config != null) {
                config.startMongo();
                config.startSql();
                new Packages(new Session("com.telifie.system", "telifie"));
//                new Voyager(true);
            }else{
                Log.error("FAILED CONFIG FILE LOAD", "CLIx110");
                System.exit(-1);
            }
        }else{
            Console.message("No config file found.");
        }
    }

    private static void importConfiguration(){
        try {
            config = new ObjectMapper().readValue(configFile, Configuration.class);
        } catch (IOException e) {
            Log.error("FAILED CONFIG.JSON IMPORT", "CLIx106");
        }
    }
}