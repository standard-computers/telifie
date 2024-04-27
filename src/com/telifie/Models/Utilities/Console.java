package com.telifie.Models.Utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telifie.Models.Actions.Search;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.PersonalClient;
import com.telifie.Models.Parser;
import com.telifie.Models.Result;
import com.telifie.Models.User;
import org.bson.Document;
import org.json.JSONObject;
import java.util.*;

public class Console {

    public static void welcome() {
        System.out.println("\n");
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
        message("More Info -> https://telifie.com/documentation");
        System.out.println("Operating System    : " + System.getProperty("os.name"));
        System.out.println("System OS Version   : " + System.getProperty("os.version"));
        System.out.println("System Architecture : " + System.getProperty("os.arch"));
        System.out.println("Working Directory : " + Telifie.configDirectory());
        System.out.println("-----------------------------------------------------------------");
    }

    public static void message(String message){
        System.out.println("-----------------------------------------------------------------");
        System.out.println(message);
        System.out.println("-----------------------------------------------------------------");
    }

    public static void string(String message){
        System.out.println(message);
    }

    public static String in(String prompt){
        System.out.print(prompt);
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    public static void command(){
        Log.flag("ENTERING CLI", "CLIx001");
        while(true){
            String cmd = Console.in("telifie -> ").trim();
            switch (cmd) {
                case "exit", "logout", "close" -> System.exit(0);
                case "@import" -> {
                    PersonalClient pc = new PersonalClient(new Session("", "telifie"));
                    ArticlesClient articles = new ArticlesClient(new Session("", "telifie"));
                    Parser p = new Parser(new Session("", "telifie"));
                    while(pc.hasNext()){
                        try {
                            String link = pc.next().getString("link");
                            Log.console("WORKING -> " + link);
                            Article a = p.parse(link);
                            if(a != null){
                                if(articles.create(a)){
                                    Log.console("ARTICLE CREATED -> https://telifie.com/articles/" + a.getId());
                                }
                            }else{
                                Log.console("-----FAILED ARTICLE-----");
                            }
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                case "@authenticate" -> {
                    Authentication auth = new Authentication(new User("", "telifie", ""));
                    Log.console("Authorizing as database admin...");
                    if(auth.authenticate()){
                        Log.flag("NEW ACCESS CREDENTIALS AUTHENTICATED", "CLIx003");
                        Log.console(new JSONObject(auth.toString()).toString(4));
                    }
                }
                case "@iplist" -> {
                    String in = Console.in("(add/remove [IP_ADDRESS]->");
                    String[] args = in.split(" ");
                    if(args[0].equals("add")){
                        Configuration.addIP(args[1]);
                    }else{
                        Configuration.removeIP(args[1]);
                    }
                }
                default -> {
                    Log.console("Querying...");
                    try {
                        Result results = new Search().execute(
                                new Session("telifie@terminal", "telifie"),
                                cmd, new Parameters(new Document("results_per_page", 1).append("pages", 1).append("page", 1))
                        );
                        if(!results.getGenerated().isEmpty()){
                            Console.message(results.getGenerated());
                        }else{
                            Console.message(new JSONObject(results.toString()).toString(4));
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}