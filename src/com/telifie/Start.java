package com.telifie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Parser;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Servers.Http;
import java.io.File;
import java.io.IOException;

public class Start {

    private static Configuration config;
    private static final File configFile = new File(Telifie.configDirectory() + "/config.json");

    public static void main(String[] args){
        Console.welcome();
        Log.message("TELIFIE STARTED", "STRx020");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.out(Event.Type.FLAG, "TELIFIE EXITED", "STRx022");
            Telifie.purgeTemp();
        }));
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            switch (mode) {
                case "--install" ->
                        install();
                case "--purge" -> {
                    Log.message("PURGE MODE ENTERED", "STRx031");
                    if (Console.in("Confirm purge, fresh install (y/n) -> ").equals("y")) {
                        if(configFile.delete()){
                            Log.out(Event.Type.DELETE, "CONFIG FILE DELETED", "STRx034");
                        }
                    }
                    System.exit(1);
                }
                case "--http" -> {
                    checkConfig();
                    try {
                        Console.log("Starting HTTP server [CONSIDER HTTPS FOR SECURITY]...");
                        new Http();
                    } catch (Exception e) {
                        Log.error("HTTP SERVER FAILED", "STRx045");
                        throw new RuntimeException(e);
                    }
                }
                case "--https" -> {
                    checkConfig();
                    try {
                        Console.log("Starting HTTPS server...");
                        new Http();
                    } catch (Exception e) {
                        Log.error("HTTPS SERVER FAILED", "STRx054");
                    }
                }
                case "--reparse" -> {
                    checkConfig();
                    new Parser(new Session("com.telifie." + Configuration.getServer_name(), "telifie")).reparse(true);
                }
                case "--worker" -> {
                    checkConfig();
                    new Parser(new Session("com.telifie." + Configuration.getServer_name(), "telifie")).reparse(false);
                }
            }
        }else{
            checkConfig();
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
                    Log.out(Event.Type.PUT, "CREATED DIRECTORY : " + folder.getPath(), "STRx075");
                }else{
                    Log.error("FAILED CREATING DIRECTORY : " + folder.getPath(), "STRx077");
                }
            }
        }
        config = new Configuration();
        config.setServer_name(Console.in("Server Name (i.e 'telifie-sv1')-> "));
        config.setMongodb(Console.in("MongoDB URI -> "));
        String sql_uri = Console.in("SQL URL -> ");
        String sql_user = Console.in("SQL Username -> ");
        String sql_psswd = Console.in("SQL Password -> ");
        config.setMysql(new Configuration.Connection(sql_uri, sql_user, sql_psswd));
        config.setEmail(Console.in("Email -> "));
        exportConfiguration();
        Log.out(Event.Type.PUT, "CONFIGURATION SAVED", "STRx090");
        System.exit(0);
    }

    private static void checkConfig(){
        if(configFile.exists()){
            Console.message("Config file found :)");
            importConfiguration();
            if (config != null) {
                config.startMongo();
                Log.message("LOADING PACKAGES...", "STRx100");
                new Packages(new Session("com.telifie.system", "telifie"));
                new Andromeda();
            }else{
                Log.error("FAILED CONFIG FILE LOAD", "STRx107");
                System.exit(-1);
            }
        }else{
            Console.message("No config file found. Use option '--install'");
            install();
        }
    }

    private static void exportConfiguration(){
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        try {
            objectWriter.writeValue(configFile, config);
            Log.out(Event.Type.PUT, "CONFIG FILE CREATED", "STRx121");
        } catch (IOException e) {
            Log.error("FAILED CONFIG.JSON EXPORT", "STRx123");
        }
    }

    private static void importConfiguration(){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            config = objectMapper.readValue(configFile, Configuration.class);
        } catch (IOException e) {
            Log.error("FAILED CONFIG.JSON IMPORT", "STRx132");
        }
    }
}