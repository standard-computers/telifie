package com.telifie.Models.Actions;

import java.io.*;

public class Out {

    public static String WINDOWS_SYSTEM_DIR = "/Program\\ Files/telifie/";
    public static String MAC_SYSTEM_DIR = System.getProperty("user.home") + "/Library/Application Support/telifie";
    public static String UNIX_SYSTEM_DIR = "/usr/bin/telifie/";

    public static void console(String message){
        System.out.println(message);
    }

    public static void line(){
        System.out.println("--------------------------------------------------------------");
    }

    public static void error(String message){
        System.err.println(message);
    }

    public static void file(String name, String content){
        File file = new File(name);
    }

    public static void serialized(String name, Serializable object){
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(name));
            out.writeObject(object);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void consoleJson(String input){
    }

    public static void telifie() {
        System.out.println("\n");
        System.out.println("||===========================================================||");
        System.out.println("||                                                           ||");
        System.out.println("||  ,--------. ,------. ,--.    ,--. ,------. ,--. ,------.  ||");
        System.out.println("||  '--.  .--' |  .---' |  |    |  | |  .---' |  | |  .---'  ||");
        System.out.println("||     |  |    |  `--,  |  |    |  | |  `--,  |  | |  `--,   ||");
        System.out.println("||     |  |    |  `---. |  '--. |  | |  |`    |  | |  `---.  ||");
        System.out.println("||     `--'    `------' `-----' `--' `--'     `--' `------'  ||");
        System.out.println("||                                                           ||");
        System.out.println("||===========================================================||\n");
    }
}
