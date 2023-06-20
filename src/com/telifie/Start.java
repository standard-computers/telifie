package com.telifie;

import com.telifie.Models.*;
import com.telifie.Models.Utilities.*;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Start {

    private static final String wrkDir = Telifie.getConfigDirectory();
    private static Configuration config;
    private static final File configFile = new File(wrkDir + "/telifie.configuration");

    public static void main(String[] args){

        Telifie.console.out.telifie();
        Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            switch (mode) {
                case "--install" ->  //First run/install
                        install();
                case "--purge" -> {  //Purge Telifie, start from scratch
                    checkConfig();
                    Telifie.console.out.string("<!----------- Purge Mode -----------!>\n");
                    if (Telifie.console.in.string("Confirm purge and fresh install (y/n) -> ").equals("y")) {
                        configFile.delete();
                        Telifie.console.out.string("telifie.configuration deleted");
                        install();
                    }
                    System.exit(1);
                }
                case "--server" -> {
                    checkConfig();
                    try {
                        new Server(config);
                    } catch (Exception e) {
                        Telifie.console.out.error("Failed to start HTTPS server...");
                        e.printStackTrace();
                    }
                }
                case "--http" -> {
                    checkConfig();
                    try {
                        Telifie.console.out.string("Starting HTTP server [CONSIDER HTTPS FOR SECURITY]...");
                        new Http(config);
                    } catch (Exception e) {
                        Telifie.console.out.error("Failed to start HTTP server...");
                        e.printStackTrace();
                    }
                }
                case "--andromeda" -> {
                    checkConfig();
                    try {
                        new Andromeda(config, true);
                    } catch (Exception e) {
                        Telifie.console.out.error("Failed to start Andromeda...");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void install(){
        File working_dir = new File(wrkDir);
        if(configFile.exists()){
            Telifie.console.out.error("telifie.configuration file already set");
            Telifie.console.out.error("Run with --purge or run normally...");
            System.exit(-1);
        }else if(!working_dir.exists()){
            boolean made_dir = working_dir.mkdirs();
            if(made_dir){
                Telifie.console.out.string("Created working directory: " + working_dir);
            }else{
                Telifie.console.out.error("Failed to create working directory: " + working_dir);
            }
        }

        Configuration configuration = new Configuration();
        Telifie.console.out.string("\nLet's connect to a MongoDB first.");
        Telifie.console.out.line();
        String mongo_uri = Telifie.console.in.string("MongoDB URI -> ");
        Domain domain = new Domain(mongo_uri);
        domain.setName("telifie");

        String email = Telifie.console.in.string("Email -> ");
        configuration.setUser(new User(email));
        configuration.setDomain(domain); //Add domain to configuration file
        configuration.setLicense(Telifie.console.in.string("Paste License -> ")); //Add license to configuration file. Must copy and paste.
        if(configuration.save(wrkDir)){
            Telifie.console.out.line();
            Telifie.console.out.string("Configuration file saved!");
            Telifie.console.out.string("Run Telifie with no arguments to start the console.");
            System.exit(0);
        }else{
            Telifie.console.out.error("Failed to save configuration file. You may have to try again :(");
            System.exit(-2);
        }
    }

    private static void checkConfig(){
        if(configFile.exists()){
            Telifie.console.out.string("Configuration file found :)");
            Telifie.console.out.line();
            config = (com.telifie.Models.Utilities.Configuration) Telifie.console.in.serialized(wrkDir + "/telifie.configuration");
        }else{
            Telifie.console.out.message("No configuration file found. Use option '--install'");
            install();
        }
    }
}
