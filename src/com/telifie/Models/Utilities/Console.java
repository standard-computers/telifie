package com.telifie.Models.Utilities;

import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Taxon;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.DraftsClient;
import com.telifie.Models.Clients.PersonalClient;
import com.telifie.Models.Parser;
import com.telifie.Models.User;
import org.bson.Document;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        System.out.println("||===========================================================||");
        System.out.println("       COPYRIGHT (C) TELIFIE LLC 2024, CINCINNATI, OHIO        ");
        message("More Info -> https://telifie.com/documentation");
        System.out.println("Operating System    : " + System.getProperty("os.name"));
        System.out.println("System OS Version   : " + System.getProperty("os.version"));
        System.out.println("System Architecture : " + System.getProperty("os.arch"));
        System.out.println("Working Directory : " + Telifie.configDirectory());
        System.out.println("---------------------------------------------------------------");
    }

    public static void message(String message){
        System.out.println("---------------------------------------------------------------");
        System.out.println(message);
        System.out.println("---------------------------------------------------------------");
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
                    String cdxFilePath = Console.in("CDX File Path Integer (local disk) -> ");
                    int s = Integer.valueOf(cdxFilePath);
                    DraftsClient drafts = new DraftsClient(new Session("", "telifie"));
                    for(int i = 0; i < 15; i++){
                        String paddedNumber = String.format("%05d", (s + i));
                        Log.console("Cooking " + (s + i) + "... [D:\\Common Crawl\\cdx\\cdx-" + paddedNumber + "]");
                        try (BufferedReader reader = new BufferedReader(new FileReader("D:\\Common Crawl\\cdx\\cdx-" + paddedNumber))) {
                            String line;
                            int inserted = 0;
                            while ((line = reader.readLine()) != null) {
                                Pattern pattern = Pattern.compile("([^ ]+) +([^ ]+) +(\\{.*\\})");
                                Matcher matcher = pattern.matcher(line);
                                if (matcher.find()) {
                                    String jsonString = matcher.group(3);
                                    JSONObject json = new JSONObject(jsonString);
                                    String url = json.getString("url");
                                    if(Asset.isValidLink(url) && !url.contains("?") && !url.contains("porn")){
                                        inserted++;
                                        if(inserted % 100000  == 0){
                                            Log.console(inserted + " -> " + url);
                                        }
                                        drafts.insert(new Document("link", url));
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    System.exit(0);
                }
                case "import" -> {
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
                case "authenticate" -> {
                    Authentication auth = new Authentication(new User("", "telifie", ""));
                    Log.console("Authorizing as database admin...");
                    if(auth.authenticate()){
                        Log.flag("NEW ACCESS CREDENTIALS AUTHENTICATED", "CLIx003");
                        Log.console(new JSONObject(auth.toString()).toString(4));
                    }
                }
                case "iplist" -> {
                    String in = Console.in("(add/remove [IP_ADDRESS]->");
                    String[] args = in.split(" ");
                    if(args[0].equals("add")){

                    }else{

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
                            Andromeda.taxon().forEach(t -> Log.console(t.getName()));
                        }else if(c.startsWith("print")){
                            String tname = c.split(" ")[1];
                            Taxon t = Andromeda.taxon(tname);
                            if(t == null){
                                Log.console("Does not exist!");
                            }else{
                                Log.console(t.items().size() + " Items");
                                Log.console(t.items().toString());
                            }
                        }else if(c.equals("count")){
                            Log.console("Total Items -> " + Andromeda.taxon().size());
                        }else if(c.startsWith("count")){
                            String tname = c.split(" ")[1];
                            Taxon t = Andromeda.taxon(tname);
                            if(t == null){
                                Log.console("Does not exist!");
                            }else{
                                Log.console("Total Items -> " + t.items().size());
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