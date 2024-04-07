package com.telifie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.Cache;
import com.telifie.Models.Clients.Packages;
import com.telifie.Models.Parser;
import com.telifie.Models.User;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Network.Http;
import org.bson.Document;
import org.json.JSONObject;
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
        Console.welcome();
        Log.message("TELIFIE STARTED", "CLIx001");
        checkConfig();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.flag("TELIFIE EXITED", "CLIx101");
            Telifie.purgeTemp();
        }));
        ArticlesClient articles = new ArticlesClient(new Session("telifie", "telifie"));
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Log.console("Preloading public domain stats in background...");
        CompletableFuture.runAsync(() -> Telifie.stats = articles.stats());
        Log.console("Scheduling worker tasks...");
        Runnable task = () -> {
            Telifie.stats = articles.stats();
            //TODO refresh packages, working cache
        };
        scheduler.scheduleAtFixedRate(task, 0, 90, TimeUnit.SECONDS);
        CompletableFuture.runAsync(() -> {
            if(Cache.history.isEmpty()){
                Log.console("Quick Response cache is empty. Warming up....");
                Log.console("Finding cache qualifying articles. This may take some time!");
                ArrayList<Article> av = articles.get(new Document("priority", new Document("$gt", 1)));
                Log.console("Committing " + av.size() + " to cache history...");
                av.forEach(a -> Cache.history.commit("telifie", a.getId(), a.getTitle(), a.getIcon(), a.getDescription()));
            }
        });

        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            switch (mode) {
                case "--install" ->
                        install();
                case "--purge" -> {
                    Log.flag("PURGE MODE ENTERED", "CLIx002");
                    if (Console.in("Confirm ('yes') -> ").equals("yes")) {
                        if(configFile.delete()){
                            Log.out(Event.Type.DELETE, "CONFIG FILE DELETED", "CLIx200");
                        }
                    }
                    System.exit(0);
                }
                case "--http" -> {
                    try {
                        Log.console("Starting HTTP server [CONSIDER HTTPS FOR SECURITY]...");
                        new Http();
                    } catch (Exception e) {
                        Log.error("HTTP SERVER FAILED", "CLIx102");
                        throw new RuntimeException(e);
                    }
                }
                case "--https" -> {
                    try {
                        Log.console("Starting HTTPS server...");
                        new Http();
                    } catch (Exception e) {
                        Log.error("HTTPS SERVER FAILED", "CLIx103");
                        throw new RuntimeException(e);
                    }
                }
                case "--master" -> new Parser(new Session("telifie", "telifie")).reparse();
                case "--authenticate" -> {
                    //TODO Create user or API/Org
                    Authentication auth = new Authentication(new User("", "telifie", ""));
                    Log.console("Authorizing as database admin...");
                    if(auth.authenticate()){
                        Log.flag("NEW ACCESS CREDENTIALS AUTHENTICATED", "CLIx003");
                        Log.console(new JSONObject(auth.toString()).toString(4));
                    }
                }
            }
        }else{
            Console.command();
        }
    }

    private static void install(){
        File[] folders = new File[]{new File(Telifie.configDirectory()), new File(Telifie.configDirectory() + "temp"), new File(Telifie.configDirectory() + "andromeda")};
        if(configFile.exists()){
            Log.console("config.json file already set");
            Console.command();
        }else{
            for(File folder : folders){
                if(folder.mkdirs()){
                    Log.put("CREATED DIRECTORY : " + folder.getPath(), "CLIx004");
                }else{
                    Log.error("FAILED CREATING DIRECTORY : " + folder.getPath(), "CLIx104");
                }
            }
        }
        String email = Console.in("Email -> ");
        String mongoUri = Console.in("MongoDB URI -> ");
        String sqlUri = Console.in("SQL URI -> ");
        config = new Configuration("v1.0.0b", email, mongoUri, sqlUri, new ArrayList<>());
        exportConfiguration();
        Log.put("CONFIGURATION SAVED", "CLIx005");
        System.exit(0);
    }

    private static void checkConfig(){
        if(configFile.exists()){
            Console.message("Config file found :)");
            importConfiguration();
            if (config != null) {
                config.startMongo();
                config.startSql();
                new Packages(new Session("com.telifie.system", "telifie"));
                new Andromeda();
            }else{
                Log.error("FAILED CONFIG FILE LOAD", "CLIx110");
                System.exit(-1);
            }
        }else{
            Console.message("No config file found. Use option '--install'");
        }
    }

    private static void exportConfiguration(){
        try {
            new ObjectMapper().writer().withDefaultPrettyPrinter().writeValue(configFile, config);
            Log.put("CONFIG FILE CREATED", "CLIx006");
        } catch (IOException e) {
            Log.error("FAILED CONFIG.JSON EXPORT", "CLIx105");
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