package com.telifie;

import com.telifie.Models.*;
import com.telifie.Models.Actions.In;
import com.telifie.Models.Actions.Out;
import com.telifie.Models.Actions.Parser;
import com.telifie.Models.Utilities.*;
import java.io.File;

public class Start {

    private static String workingDirectory;
    private static Configuration configuration = null;
    private static File configuration_file;

    public static void main(String[] args){
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

        //Get Operating Tool information
        String operatingSystem = System.getProperty("os.name");
        Out.console("Operating System : " + operatingSystem);
        Out.console("System Architecture : " + System.getProperty("os.arch"));
        if(operatingSystem.equals("Mac OS X")){

            workingDirectory = Out.MAC_SYSTEM_DIR;
        }else if(operatingSystem.startsWith("Windows")){

            workingDirectory = Out.WINDOWS_SYSTEM_DIR;
        }else{

            workingDirectory = Out.UNIX_SYSTEM_DIR;
        }
        configuration_file = new File(workingDirectory + "/telifie.configuration");

        if (args.length > 0) {

            if(args[0].equals("--install")){ //First run/install

                install();

            }else if(args[0].equals("--purge")){ //Purge Telifie, start from scratch

                Out.console("<!----------- Purge Mode -----------!>\n");
                if(In.string("Confirm purge and fresh install (y/n) -> ").equals("y")){

                    configuration_file.delete();
                    Out.console("telifie.configuration deleted.");
                    install();
                }

                System.exit(1);
            }else if(args[0].equals("--parser")){

                Out.console("<!---------- Parser Mode ----------!>\n");
                String in = In.string("URI/URL -> ");
                while(!in.equals("q")){

                    Parser.engines.parse(in);
                    in = In.string("URI/URL -> ");
                }
            }else if(args[0].equals("--server")){

                Out.console("Starting HTTP server...");
                new Server(false);
            }

            System.err.println("\nInvalid argument provided...\n");
            System.exit(-1);
        }else{

            Out.line();
            Out.console("Searching for telifie.configuration file...");
            if(configuration_file.exists()){
                Out.console("Configuration file found :)");
                configuration = (com.telifie.Models.Utilities.Configuration) In.serialized(workingDirectory + "/telifie.configuration");
                Out.line();
                console();
            }

            Out.error("No configuration file found. Use option '--install'");
            System.exit(-1);
        }
    }

    private static void install(){

        File working_dir = new File(workingDirectory);
        if(configuration_file.exists()){
            Out.error("telifie.configuration file already set");
            Out.error("Run with --purge or run normally...");
            System.exit(-1);
        }else if(!working_dir.exists()){
            boolean made_dir = working_dir.mkdirs();
            if(made_dir){
                Out.console("Created working directory: " + working_dir);
            }else{
                Out.error("Failed to create working directory: " + working_dir);
            }
        }

        Configuration configuration = new Configuration();

        //Connect to database
        Out.console("\nLet's connect to a MongoDB first.");
        Out.line();
        String mongo_uri = In.string("MongoDB URI -> ");
        Domain domain = new Domain(mongo_uri);

        if(In.string("Local install? (y/n) -> ").equals("y")){
            String email = In.string("Email -> ");
            configuration.setUser(new User(email));
        }else{
            Network network = new Network();
            String email = null;
            while(network.getStatusCode() != 200){
                email = In.string("Email -> ");
                Out.console("Looking up account...");
                network.get("https://telifie.net/users/search/" + email);
                //TODO Include information bellow with Bearer
                // "app_id=com.telifie.Start.install&auth_token=&email=" + email
                Out.console("[ HTTP RESPONSE CODE / USER / SEARCH ] " + network.getStatusCode());
            }

            //Authenticate account with auth code. Request 2FA code
            User user = new User(email);
            user.requestAuthenticationCode();
            String psswd = In.string("One-time Security Code (2FA) -> ");

            while(!user.verify(psswd)){

                user.requestAuthenticationCode(); //Request new 2FA code

                Out.console("<!--------- INCORRECT CODE PROVIDED ----------->");
                Out.console("New code sent to primary method...");
                Out.line();
                psswd = In.string("One-time Security Code (2FA) -> ");

            }
            Out.line();
            //TODO Check license validity with .com
            configuration.setUser(user); //Add user to configuration file
        }

        configuration.addDomain(domain); //Add domain to configuration file
        configuration.setLicense(In.string("Paste License -> ")); //Add license to configuration file. Must copy and paste.

        if(configuration.save(workingDirectory)){
            Out.line();
            Out.console("Configuration file saved!");
        }else{
            Out.error("Failed to save configuration file. You may have to try again :(");
            System.exit(-2);
        }

    }

    private static void console(){
        //TODO Get preferences and user information
        while(true){
            String targetDomain = "telifie";
            String input = In.string(targetDomain + "://");
            Command command = new Command(targetDomain + "://" + input);
            command.parseCommand(configuration);
        }
    }
}
