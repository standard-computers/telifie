package com.telifie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.telifie.Models.Utilities.*;
import java.io.File;
import java.io.IOException;

public class Start {

    private static final String wrkDir = Telifie.configDirectory();
    private static Configuration config;
    private static final File configFile = new File(wrkDir + "/config.json");

    public static void main(String[] args){
        Console.out.welcome();
        Log.out(Event.Type.MESSAGE, "TELIFIE STARTED");
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            switch (mode) {
                case "--install" ->
                        install();
                case "--purge" -> {
                    Log.out(Event.Type.MESSAGE, "PURGE MODE ENTERED");
                    if (Console.in.string("Confirm purge, fresh install (y/n) -> ").equals("y")) {
                        configFile.delete();
                        Log.out(Event.Type.DELETE, "telifie.configuration deleted");
                    }
                    System.exit(1);
                }
                case "--http" -> {
                    checkConfig();
                    try {
                        System.out.println("Starting HTTP server [CONSIDER HTTPS FOR SECURITY]...");
                        new Http(config);
                    } catch (Exception e) {
                        Log.error("Failed to start HTTP server");
                        e.printStackTrace();
                    }
                }
            }
        }else{
            checkConfig();
            new Console.command(config);
        }
    }

    private static void install(){
        File wd = new File(wrkDir);
        if(configFile.exists()){
            System.err.println("config.json file already set");
            System.err.println("Run with --purge or run normally");
            System.exit(-1);
        }else if(!wd.exists()){
            boolean made_dir = wd.mkdirs();
            if(made_dir){
                Log.out(Event.Type.PUT, "CREATED WORKING DIRECTORY : " + wd);
            }else{
                Log.error("FAILED CREATING WORKING DIRECTORY : " + wd);
            }
        }
        config = new Configuration();
        config.setMongodb(Console.in.string("MongoDB URI -> "));
        config.setEmail(Console.in.string("Email -> "));
        exportConfiguration();
        Console.out.line();
        System.out.println("Configuration saved!\nRun Telifie with no arguments to start the console.");
        System.exit(0);
    }

    private static void checkConfig(){
        if(configFile.exists()){
            Console.out.message("Config file found :)");
            importConfiguration();
            if (config != null) {
                config.startMongo();
            }else{
                Log.error("FAILED CONFIG FILE LOAD");
                System.exit(-1);
            }
        }else{
            Console.out.message("No config file found. Use option '--install'");
            install();
        }
    }

    private static void exportConfiguration(){
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        try {
            objectWriter.writeValue(configFile, config);
            Log.out(Event.Type.PUT, "config.json written");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void importConfiguration(){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            config = objectMapper.readValue(configFile, Configuration.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}