package com.telifie.Models.Utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telifie.Models.Clients.Search;
import com.telifie.Models.Article;
import com.telifie.Models.Result;
import com.telifie.Models.User;
import org.bson.Document;
import org.json.JSONObject;
import java.util.*;

public class Console {

    public static void message(String m){
        System.out.println("-----------------------------------------------------------------");
        System.out.println(m);
        System.out.println("-----------------------------------------------------------------");
    }

    public static String in(String p){
        System.out.print(p);
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    public static void command(){
        Log.message("ENTERING CLI", "CLIx001");
        while(true){
            String cmd = Console.in("telifie -> ").trim();
            switch (cmd) {
                case "exit", "logout", "close" -> System.exit(0);
                case "@authenticate" -> {
                    Authentication auth = new Authentication(new User("", "telifie", ""));
                    Log.console("Authorizing as database admin...");
                    if(auth.authenticate()){
                        Log.flag("NEW ACCESS CREDENTIALS AUTHENTICATED", "CLIx003");
                        Log.console(new JSONObject(auth.toString()).toString(4));
                    }
                }
                default -> {
                    Log.console("Thinking...");
                    try {
                        Result results = new Search().execute(
                                new Session("telifie@terminal", "telifie"),
                                cmd, new Parameters(new Document("results_per_page", 1).append("pages", 1).append("page", 1))
                        );
                        if(!results.getGenerated().isEmpty()){
                            Console.message(results.getGenerated());
                        }else if(results.getResults() != null){
                            ArrayList<Article> as = (ArrayList<Article>) results.getResults();
                            Log.wrap(as.get(0).getContent(), 65);
                        }else{
                            Log.wrap(new JSONObject(results.toString()).toString(4), 65);
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}