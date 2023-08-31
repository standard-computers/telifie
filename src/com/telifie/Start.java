package com.telifie;

import com.telifie.Models.*;
import com.telifie.Models.Utilities.*;
import java.io.File;
import java.util.Scanner;

public class Start {

    private static final String wrkDir = Telifie.getConfigDirectory();
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
                    checkConfig();
                    System.out.println("<!----------- Purge Mode -----------!>\n");
                    if (Console.in.string("Confirm purge, fresh install (y/n) -> ").equals("y")) {
                        configFile.delete();
                        System.out.println("telifie.configuration deleted");
                        install();
                    }
                    System.exit(1);
                }
                case "--server" -> {
                    checkConfig();
                    try {
                        new Server(config);
                    } catch (Exception e) {
                        System.err.println("Failed to start HTTPS server...");
                        e.printStackTrace();
                    }
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
                case "--andromeda" -> {
                    checkConfig();
                    try {
                        Andromeda a = new Andromeda(config);
                        Scanner in = new Scanner(System.in);
                        System.out.println("Andromeda started.\nname:item,item,item,...");
                        while(true){
                            String c = in.nextLine();
                            String[] ts = c.split(":");
                            String name = ts[0];
                            String[] items = ts[1].split(",");
                            for(String item : items){
                                if(!item.trim().equals("")){
                                    a.add(name, item.trim().toLowerCase().replace(".",""));
                                }
                            }
                            System.out.println("[DONE]");
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to start Andromeda...");
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
        System.out.println("\nLet's connect to a MongoDB first.");
        Console.out.line();
        String mongo_uri = Console.in.string("MongoDB URI -> ");
        configuration.setUri(mongo_uri);
        Domain domain = new Domain();
        domain.setName("telifie");
        String email = Console.in.string("Email -> ");
        configuration.setUser(new User(email));
        configuration.setDomain(domain); //Add domain to configuration file
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
        }else{
            Console.out.message("No config file found. Use option '--install'");
            install();
        }
    }
}