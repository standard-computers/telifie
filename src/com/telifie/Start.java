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
                        install();
                    }
                    System.exit(1);
                }
                case "--http" -> {
                    checkConfig();
                    try {
                        System.out.println("Starting HTTP server [CONSIDER HTTPS FOR SECURITY]...");
                        new Http(config);
                    } catch (Exception e) {
                        System.err.println("Failed to start HTTP server...");
                        e.printStackTrace();
                    }
                }
            }
        }else{
            checkConfig();
            new Console.line(config);
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
                System.out.println("Created working directory: " + working_dir);
            }else{
                System.err.println("Failed to create working directory: " + working_dir);
            }
        }
        Configuration configuration = new Configuration();
        System.out.println("\nLet's connect to a MongoDB.");
        Console.out.line();
        Console.out.string("Remote or local installation?");
        int choice = Console.in.integer("(1: LOCAL / 2: REMOTE) -> ");
        configuration.setInstallation(choice == 1 ? "LOCAL" : "REMOTE");
        String mongoUri = Console.in.string("Mongo URI -> ");
        configuration.setMongoURI(mongoUri);
        String email = Console.in.string("Email -> ");
        configuration.setUser(new User(email));
        configuration.setLicense(Console.in.string("Paste License -> ")); //Add license to configuration file. Must copy and paste.
        if(configuration.save(wrkDir)){
            Console.out.line();
            System.out.println("Configuration saved!\nRun Telifie with no arguments to start the console.");
            System.exit(0);
        }else{
            System.err.println("Failed to save configuration file. You may have to try again :(");
            System.exit(-2);
        }
    }

    private static void checkConfig(){
        if(configFile.exists()){
            System.out.println("Configuration file found :)");
            Console.out.line();
            config = (com.telifie.Models.Utilities.Configuration) Console.in.serialized(wrkDir + "/telifie.configuration");
            config.startMongo();
        }else{
            Console.out.message("No config file found. Use option '--install'");
            System.exit(-1);
        }
    }
}