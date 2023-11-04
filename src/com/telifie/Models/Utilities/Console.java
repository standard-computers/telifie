package com.telifie.Models.Utilities;

import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Taxon;

import java.util.Scanner;

public class Console {

    public static void welcome() {
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
        String operatingSystem = System.getProperty("os.name");
        System.out.println("Operating System : " + operatingSystem);
        System.out.println("System Architecture : " + System.getProperty("os.arch"));
        Console.line();
    }

    public static void line(){
        System.out.println("--------------------------------------------------------------");
    }

    public static void message(String message){
        line();
        System.out.println(message);
        line();
    }

    public static void log(String message){
        System.out.println(message);
    }

    public static void string(String message){
        System.out.println(message);
    }

    public static String in(){
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    /**
     * Accepts String for prompt.
     * Returns String of users input.
     * @param prompt Prompt to user for requested input.
     * @return Users input.
     */
    public static String in(String prompt){
        System.out.print(prompt);
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    public static void command(){
        while(true){
            String cmd = Console.in("telifie -> ");
            switch (cmd) {
                case "exit", "logout", "close" -> System.exit(1);
                case "http" -> {
                    try {
                        new Http();
                    } catch (Exception e) {
                        Log.error("Failed to start HTTP server");
                    }
                }
                case "andromeda" -> {
                    Andromeda andromeda = new Andromeda();
                    boolean loop = true;
                    while(loop){
                        String c = Console.in("telifie -> andromeda -> ");
                        if(c.equals("add")){
                            String tn = Console.in("Taxon Name -> ");
                            String[] ti = Console.in("Taxon Items -> ").split(",");
                            for(String i : ti){
                                andromeda.add(tn, i.trim().toLowerCase().replaceAll("'", ""));
                            }
                        }else if(c.equals("print")){
                            andromeda.taxon().forEach(t -> Console.log(t.getName()));
                        }else if(c.startsWith("print")){
                            String tname = c.split(" ")[1];
                            Taxon t = andromeda.taxon(tname);
                            Console.log(t.items().toString());
                        }else if(c.equals("exit")){
                            loop = false;
                        }
                    }
                }
            }
        }
    }
}