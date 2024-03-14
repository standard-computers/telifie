package com.telifie.Models.Utilities;

import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Taxon;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.DraftsClient;
import com.telifie.Models.Clients.PersonalClient;
import com.telifie.Models.Parser;
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
                    String cdxFilePath = Console.in("CDX File Path Integer (local disk) -> ");
                    int s = Integer.valueOf(cdxFilePath);
                    DraftsClient drafts = new DraftsClient(new Session("", "telifie"));
                    for(int i = 0; i < 15; i++){
                        String paddedNumber = String.format("%05d", (s + i));
                        Console.log("Cooking " + (s + i) + "... [D:\\Common Crawl\\cdx\\cdx-" + paddedNumber + "]");
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
                                            Console.log(inserted + " -> " + url);
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
                case "personal" -> {
                    String file = Console.in("File Path ->");
                    PersonalClient pc = new PersonalClient(new Session("", "telifie"));
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        int i = 0;
                        String line;
                        while ((line = reader.readLine()) != null) {
                            i++;
                            if(!line.contains("List_of")){
                                Console.log(String.valueOf(i));
                                pc.insert(new Document("link", line));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                case "import" -> {
                    String file = Console.in("File Path ->");
                    PersonalClient pc = new PersonalClient(new Session("", "telifie"));
                    ArticlesClient articles = new ArticlesClient(new Session("", "telifie"));
                    Parser p = new Parser(new Session("", "telifie"));
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if(!line.contains("List_of")){
                                if(articles.withSource(line) == null){
                                    Thread.sleep(3000);
                                    Article a = p.parse(line);
                                    if(a != null){
                                        articles.create(a);
                                        Console.log("ARTICLE CREATED -> https://telifie.com/articles/" + a.getId());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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
                        }else if(c.equals("count")){
                            Console.log("Total Items -> " + Andromeda.taxon().size());
                        }else if(c.startsWith("count")){
                            String tname = c.split(" ")[1];
                            Taxon t = Andromeda.taxon(tname);
                            if(t == null){
                                Console.log("Does not exist!");
                            }else{
                                Console.log("Total Items -> " + t.items().size());
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