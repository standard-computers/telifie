package com.telifie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Network.Http;
import com.telifie.Models.Utilities.Network.Https;
import java.io.File;
import java.io.IOException;

public class Start {

    private static Configuration config;
    private static final File configFile = new File(Telifie.configDirectory() + "/config.json");

    public static void main(String[] args){
        System.out.println("||=============================================================||");
        System.out.println("||                                                             ||");
        System.out.println("||   ,--------. ,------. ,--.    ,--. ,------. ,--. ,------.   ||");
        System.out.println("||   '--.  .--' |  .---' |  |    |  | |  .---' |  | |  .---'   ||");
        System.out.println("||      |  |    |  `--,  |  |    |  | |  `--,  |  | |  `--,    ||");
        System.out.println("||      |  |    |  `---. |  '--. |  | |  |`    |  | |  `---.   ||");
        System.out.println("||      `--'    `------' `-----' `--' `--'     `--' `------'   ||");
        System.out.println("||                                                             ||");
        System.out.println("||=============================================================||");
        System.out.println("        COPYRIGHT (C) TELIFIE LLC 2024, CINCINNATI, OHIO         ");
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Operating System    : " + System.getProperty("os.name"));
        System.out.println("System OS Version   : " + System.getProperty("os.version"));
        System.out.println("System Architecture : " + System.getProperty("os.arch"));
        System.out.println("Working Directory   : " + Telifie.configDirectory());
        System.out.println("-----------------------------------------------------------------");
        Log.message("TELIFIE STARTED", "CLIx100");
        File[] folders = new File[]{new File(Telifie.configDirectory()), new File(Telifie.configDirectory() + "\\temp"), new File(Telifie.configDirectory() + "\\models")};
        for(File folder : folders){
            if(!folder.exists()){
                if(folder.mkdirs()){
                    Log.out(Event.Type.PUT, "CREATED DIRECTORY : " + folder.getPath(), "CLIx004");
                }else{
                    Log.error("FAILED CREATING DIRECTORY : " + folder.getPath(), "CLIx104");
                }
            }
        }
        if(configFile.exists()){
            Console.message("Config file found :)");
            try {
                config = new ObjectMapper().readValue(configFile, Configuration.class);
            } catch (IOException e) {
                Log.error("FAILED CONFIG.JSON IMPORT", "CLIx106");
            }
            if (config != null) {
                config.connectDB();
            }else{
                Log.error("FAILED CONFIG FILE LOAD", "CLIx110");
                System.exit(-1);
            }
        }else{
            Console.message("No config file found.");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.out(Event.Type.FLAG, "TELIFIE EXITED", "CLIx101");
            File tempDir = new File(Telifie.configDirectory() + "temp");
            if(tempDir.exists() && tempDir.isDirectory()){
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            if (!file.delete()) {
                                Log.console("FAILED TO DELETE FILE : " + file.getName());
                            }
                        }
                    }
                }
            }
        }));
        if (args.length > 0) {
            new Cognition();
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
                        new Https();
                    } catch (Exception e) {
                        Log.error("HTTPS SERVER FAILED", "CLIx103");
                    }
                } //CompletableFuture.runAsync(() -> {})
            }
        }else{
            Console.command();
        }
    }
}