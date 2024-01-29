package com.telifie.Models.Utilities;

import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Taxon;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.User;
import com.telifie.Models.Utilities.Servers.Http;
import org.bson.Document;
import org.json.JSONObject;
import java.util.*;

public class Console {

    public static void welcome() {
        Console.log("\n");
        Console.log("||===========================================================||");
        Console.log("||                                                           ||");
        Console.log("||  ,--------. ,------. ,--.    ,--. ,------. ,--. ,------.  ||");
        Console.log("||  '--.  .--' |  .---' |  |    |  | |  .---' |  | |  .---'  ||");
        Console.log("||     |  |    |  `--,  |  |    |  | |  `--,  |  | |  `--,   ||");
        Console.log("||     |  |    |  `---. |  '--. |  | |  |`    |  | |  `---.  ||");
        Console.log("||     `--'    `------' `-----' `--' `--'     `--' `------'  ||");
        Console.log("||                                                           ||");
        Console.log("||===========================================================||");
        Console.log("       COPYRIGHT (C) TELIFIE LLC 2024, CINCINNATI, OHIO        ");
        Console.message("More Info -> https://telifie.com/documentation");
        Console.log("Operating System    : " + System.getProperty("os.name"));
        Console.log("System OS Version   : " + System.getProperty("os.version"));
        Console.log("System Architecture : " + System.getProperty("os.arch"));
        Console.log("Working Directory : " + Telifie.configDirectory());
        Console.line();
    }

    public static void line(){
        System.out.println("---------------------------------------------------------------");
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

    public static String in(String prompt){
        System.out.print(prompt);
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    public static void command(){
        Log.flag("ENTERING CLI", "CLIx001");
        while(true){
            String cmd = Console.in("telifie -> ");
            switch (cmd) {
                case "exit", "logout", "close" -> System.exit(0);
                case "routine" -> {
                    ArticlesClient articles = new ArticlesClient(new Session("telifie." + Configuration.getServer_name(), "telifie"));
                    ArrayList<Article> as = articles.get(new Document("$and", Arrays.asList(
                            new Document("description", "Color"),
                            new Document("attributes.value", "v8j7l5")
                        )));
                    Console.log("Found = " + as.size());
                    for (Article a : as) {
                        if (articles.update(a, a)) {
                            Console.log("Article Updated");
                        }
                    }
                }
                case "andromeda" -> {
                    boolean loop = true;
                    while(loop){
                        String c = Console.in("telifie -> andromeda -> ");
                        if(c.equals("add")){
                            String tn = Console.in("Taxon Name -> ");
                            String[] ti = Console.in("Taxon Items -> ").split(",");
                            for(String i : ti){
                                Andromeda.add(tn, i.trim().toLowerCase().replaceAll("'", ""));
                            }
                            Andromeda.save();
                        }else if(c.equals("print")){
                            Andromeda.taxon().forEach(t -> Console.log(t.getName()));
                        }else if(c.startsWith("print")){
                            String tname = c.split(" ")[1];
                            Taxon t = Andromeda.taxon(tname);
                            if(t == null){
                                Console.log("Does not exist!");
                            }else{
                                Console.log(t.items().size() + " Items");
                                Console.log(t.items().toString());
                            }
                        }else if(c.equals("index")){
                            Andromeda.index();
                        }else if(c.equals("exit")){
                            loop = false;
                        }
                    }
                }
            }
        }
    }
}