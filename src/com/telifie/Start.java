package com.telifie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.telifie.Models.Utilities.*;
import java.io.File;
import java.io.IOException;

public class Start {

    private static Configuration config;
    private static final File configFile = new File(Telifie.configDirectory() + "/config.json");

    public static void main(String[] args){
        Console.welcome();
        Log.out(Event.Type.MESSAGE, "TELIFIE STARTED");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.out(Event.Type.FLAG, "TELIFIE EXITED");
            Telifie.purgeTemp();
        }));
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            switch (mode) {
                case "--install" ->
                        install();
                case "--purge" -> {
                    Log.out(Event.Type.MESSAGE, "PURGE MODE ENTERED");
                    if (Console.in("Confirm purge, fresh install (y/n) -> ").equals("y")) {
                        if(configFile.delete()){
                            Log.out(Event.Type.DELETE, "CONFIG FILE DELETED");
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
                        Log.error("HTTP SERVER FAILED");
                    }
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
                    Log.out(Event.Type.PUT, "CREATED DIRECTORY : " + folder.getPath());
                }else{
                    Log.error("FAILED CREATING DIRECTORY : " + folder.getPath());
                }
            }
        }
        config = new Configuration();
        config.setMongodb(Console.in("MongoDB URI -> "));
        config.setEmail(Console.in("Email -> "));
        exportConfiguration();
        Log.out(Event.Type.PUT, "CONFIGURATION SAVED");
        System.exit(0);
    }

    private static void checkConfig(){
        if(configFile.exists()){
            Console.message("Config file found :)");
            importConfiguration();
            if (config != null) {
                config.startMongo();
            }else{
                Log.error("FAILED CONFIG FILE LOAD");
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
            Log.out(Event.Type.PUT, "CONFIG FILE CREATED");
        } catch (IOException e) {
            Log.error("FAILED CONFIG.JSON EXPORT");
        }
    }

    private static void importConfiguration(){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            config = objectMapper.readValue(configFile, Configuration.class);
        } catch (IOException e) {
            Log.error("FAILED CONFIG.JSON IMPORT");
        }
    }
}