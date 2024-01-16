package com.telifie.Models.Utilities;

import com.telifie.Models.Actions.Search;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Taxon;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Utilities.Servers.Http;
import org.bson.Document;
import org.checkerframework.checker.units.qual.A;

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
        Console.line();
        Console.log("More Info -> https://telifie.com/documentation");
        Console.line();
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
        Log.out(Event.Type.FLAG, "ENTERING CLI", "CLIx062");
        while(true){
            String cmd = Console.in("telifie -> ");
            switch (cmd) {
                case "exit", "logout", "close" -> System.exit(0);
                case "http" -> {
                    try {
                        new Http();
                    } catch (Exception e) {
                        Log.error("Failed to start HTTP server", "CLIx071");
                    }
                }
                case "routine" -> {
                    ArticlesClient articles = new ArticlesClient(new Session("telifie." + Configuration.getServer_name(), "telifie"));
                    ArrayList<Article> as = articles.get(new Document("$and", Arrays.asList(
                            new Document("description", "Color"),
                            new Document("attributes.value", "v8j7l5")
                        )));
                    Console.log("Found = " + as.size());
                    for(int i = 0; i < as.size(); i++){
                        Article a = as.get(i);
                        a.setIcon("https://singlecolorimage.com/get/" + a.getAttribute("Hex").replace("#", "") + "/64x64.png");
                        Console.log(a.toString());
                        if( articles.update(a, a)){
                            Console.log("Article Updated");
                        }
                    }
                }
                case "reporting" -> {
                    Console.message("PLEASE DO NOT EXIT OR IT WILL NOT FINISH");
                    Console.message("Access domain stats report through web or API");
                    ArticlesClient articles = new ArticlesClient(new Session("com.telifie." + Configuration.getServer_name(), "telifie"));
                    ArrayList<Article> all = articles.get(new Document());
                    Map<String, Integer> duplicatedLinks = new HashMap<>();
                    Map<String, Integer> duplicatedTitles = new HashMap<>();
                    for (Article article : all) {
                        String link = article.getLink();
                        String title = article.getTitle();
                        if (link != null && !link.isEmpty()) {
                            duplicatedLinks.put(link, duplicatedLinks.getOrDefault(link, 0) + 1);
                        }
                        if (title != null && !title.isEmpty()) {
                            duplicatedTitles.put(title, duplicatedTitles.getOrDefault(title, 0) + 1);
                        }
                    }
                    Console.log("Creating duplicate LINKS report...");
                    new Files("reports/duplicate_links.csv");
                    Files.csv.append("Link,Count");
                    for (Map.Entry<String, Integer> entry : duplicatedLinks.entrySet()) {
                        Files.csv.append(entry.getKey() + "," + entry.getValue());
                    }

                    Console.log("Creating duplicate TITLES report...");
                    new Files("reports/duplicate_titles.csv");
                    Files.csv.append("Title,Count");
                    for (Map.Entry<String, Integer> entry : duplicatedTitles.entrySet()) {
                        Files.csv.append(entry.getKey() + "," + entry.getValue());
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
                                Console.log(t.items().toString());
                            }
                        }else if(c.equals("index")){
                            Andromeda.index();
                        }else if(c.equals("exit")){
                            loop = false;
                        }else{ //TODO Accept input to 'Search' and allow input/output of JSON
                            new Search().execute(new Session("com.telifie.app", "telifie"), c, new Parameters(new Document()));
                        }
                    }
                }
            }
        }
    }
}