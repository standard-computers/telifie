package com.telifie.Models.Utilities;

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
                case "exit", "logout", "close", "bye" -> System.exit(0);
                case "@auth" -> {
                    Authentication auth = new Authentication(new User("", "telifie", ""));
                    Log.console("Authorizing as database admin...");
                    if(auth.authenticate()){
                        Log.out(Event.Type.FLAG, "NEW ACCESS CREDENTIALS AUTHENTICATED", "CLIx003");
                        Log.console(new JSONObject(auth.toString()).toString(4));
                    }
                }
                case "$" -> new Cognition(Configuration.model);
                default -> {
                    Log.console("Thinking...");
                    Result r = new Command("/").parseCommand(new Session("telifie@terminal", "telifie"), new Document("query", cmd), "POST");
                    if(r.getResults() != null || !r.getGenerated().isEmpty()){
                        ArrayList<Article> as = (ArrayList<Article>) r.getResults();
                        Log.wrap(as.get(0).getContent());
                    }else{
                        Log.console("Couldn't find anything :(");
                    }
                }
            }
        }
    }
}