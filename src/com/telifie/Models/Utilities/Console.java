package com.telifie.Models.Utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Scanner;

public class Console {

    public static class out {
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
            Console.out.line();
        }

        public static void line(){
            System.out.println("--------------------------------------------------------------");
        }

        public static void message(String message){
            line();
            System.out.println("\n" + message + "\n");
            line();
        }
    }

    public static class in {

        public static String string(){
            Scanner in = new Scanner(System.in);
            return in.nextLine();
        }

        public static String string(String prompt){
            System.out.print(prompt);
            Scanner in = new Scanner(System.in);
            return in.nextLine();
        }

        public static Object serialized(String dir){
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(dir));
                return in.readObject();
            } catch (IOException e) {
                return null;
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    public static class line {

        public line(Configuration config){
            while(true){
                String cmd = Console.in.string("telifie > ");
                if(cmd.equals("exit")){
                    System.exit(1);
                }else if(cmd.equals("http")){
                    try {
                        new Http(config);
                    } catch (Exception e) {
                        System.err.println("Failed to start HTTP server...");
                        e.printStackTrace();
                    }
                }else if(cmd.equals("geocode")){
                    try {
                        Telifie.tools.geocode(config);
                    } catch (Exception e) {
                        System.err.println("Failed to start Geocode server...");
                        e.printStackTrace();
                    }
                }else if(cmd.equals("clean")){

                }else if(cmd.equals("sepimg")){

                }
            }
        }
    }
}
