package com.telifie;

import com.telifie.Models.*;
import com.telifie.Models.Utilities.*;
import java.io.File;

public class Start {

    private static final String wrkDir = Telifie.configDirectory();
    private static Configuration config;
    private static final File configFile = new File(wrkDir + "/telifie.configuration");

    public static void main(String[] args){
        Console.out.welcome();
        Log.out(Event.Type.MESSAGE, "Telifie started");
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            switch (mode) {
                case "--install" ->
                        install();
                case "--purge" -> {
                    System.out.println("<!----------- Purge Mode -----------!>\n");
                    if (Console.in.string("Confirm purge, fresh install (y/n) -> ").equals("y")) {
                        configFile.delete();
                        System.out.println("telifie.configuration deleted");
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
        File working_dir = new File(wrkDir);
        if(configFile.exists()){
            System.err.println("telifie.configuration file already set");
            System.err.println("Run with --purge or run normally");
            System.exit(-1);
        }else if(!working_dir.exists()){
            boolean made_dir = working_dir.mkdirs();
            if(made_dir){
                Log.out(Event.Type.PUT, "Created working directory: " + working_dir);
            }else{
                Log.error("Failed to create working directory: " + working_dir);
            }
        }
        Configuration configuration = new Configuration();
        Console.out.message("\nLet's connect to a MongoDB.");
        String mongoUri = Console.in.string("Mongo URI -> ");
        configuration.setMongoURI(mongoUri);
        String email = Console.in.string("Email -> ");
        configuration.setUser(new User(email));
        if(configuration.save(wrkDir)){
            Console.out.line();
            System.out.println("Configuration saved!\nRun Telifie with no arguments to start the console.");
            System.exit(0);
        }else{
            Log.error("Failed to save configuration file.");
            System.exit(-2);
        }
    }

    private static void checkConfig(){
        if(configFile.exists()){
            Console.out.message("Configuration file found :)");
            config = (com.telifie.Models.Utilities.Configuration) Console.in.serialized(wrkDir + "/telifie.configuration");
            if (config != null) {
                config.startMongo();
            }else{
                Log.error("Failed to load configuration file.");
            }
        }else{
            Console.out.message("No config file found. Use option '--install'");
            System.exit(-1);
        }
    }
}