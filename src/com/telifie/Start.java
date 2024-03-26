package com.telifie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Clients.Packages;
import com.telifie.Models.Parser;
import com.telifie.Models.User;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Network.Http;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            switch (mode) {
                case "--install" ->
                        install();
                case "--purge" -> {
                    Log.message("PURGE MODE ENTERED", "CLIx002");
                    if (Console.in("Confirm ('yes') -> ").equals("yes")) {
                        if(configFile.delete()){
                            Log.out(Event.Type.DELETE, "CONFIG FILE DELETED", "CLIx200");
                        }
                    }
                    System.exit(0);
                }
                case "--http" -> {
                    try {
                        Console.log("Starting HTTP server [CONSIDER HTTPS FOR SECURITY]...");
                        new Http();
                    } catch (Exception e) {
                        Log.error("HTTP SERVER FAILED", "CLIx102");
                        throw new RuntimeException(e);
                    }
                }
                case "--https" -> {
                    try {
                        Console.log("Starting HTTPS server...");
                        new Http();
                    } catch (Exception e) {
                        Log.error("HTTPS SERVER FAILED", "CLIx103");
                        throw new RuntimeException(e);
                    }
                }
                case "--node" -> new Parser(new Session("telifie", "telifie")).reparse();
                case "--authenticate" -> {
                    //TODO Create user or API/Org
                    Authentication auth = new Authentication(new User("", "telifie", ""));
                    Console.log("Authorizing as database admin...");
                    if(auth.authenticate()){
                        Log.flag("NEW ACCESS CREDENTIALS AUTHENTICATED", "CLIx003");
                        Console.log(new JSONObject(auth.toString()).toString(4));
                    }
                }
            }
        }else{
            Console.command();
        }
    }

    private static void install(){
        File[] folders = new File[]{
                new File(Telifie.configDirectory()),
                new File(Telifie.configDirectory() + "temp"),
                new File(Telifie.configDirectory() + "andromeda"),
        };
        if(configFile.exists()){
            Console.log("config.json file already set");
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