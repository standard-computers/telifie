package com.telifie.Models.Andromeda;

import com.google.common.html.HtmlEscapers;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Console;
import org.apache.commons.text.StringEscapeUtils;
import org.bson.Document;
import java.io.*;
import java.util.*;

public class Andromeda {

    private static ArrayList<Taxon> taxon = new ArrayList<>();
    public static final String[] ALPHAS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    public static final String[] NUMERALS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    public static final String[] PROXIMITY = {"near", "nearby", "close to", "around", "within", "in the vicinity of", "within walking distance of", "adjacent to", "bordering", "neighboring", "local to", "surrounding", "not far from", "just off"};

    public Andromeda(){
        Log.message("INITIALIZING ANDROMEDA");
        if(new File(Telifie.configDirectory() + "andromeda/taxon.telifie").exists()){
            try (FileInputStream fileIn = new FileInputStream(Telifie.configDirectory() + "andromeda/taxon.telifie");
                 ObjectInputStream in = new ObjectInputStream(fileIn)) {
                this.taxon = (ArrayList<Taxon>) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void index(){
        ArticlesClient articles = new ArticlesClient(new Session("com.telifie.master_data_team", "telifie"));
        ArrayList<Article> al = articles.withProjection(new Document("$and", Arrays.asList(
                new Document("link", new Document("$ne", null)),
                new Document("description", new Document("$ne", "Image")),
                new Document("description", new Document("$ne", "Definition")),
                new Document("description", new Document("$ne", "Webpage"))
        )), new Document("icon", 1)
                .append("title", 1)
                .append("description", 1)
                .append("link", 1)
                .append("_id", 0));
        int it = al.size();
        Console.log("ESTIMATED INDEX : " + it);
        final int[] i = {0};
        al.forEach(a -> {
            i[0]++;
            Console.log("INDEXING " + i[0] + "/" + it);
            String t = a.getDescription().toLowerCase().trim();
            String tn = a.getTitle().trim().toLowerCase();
            add(t, tn);
        });
        save();
    }

    public static Taxon taxon(String name) {
        for (Taxon t : taxon) {
            if (t.getName().equals(name.toLowerCase().trim())) {
                return t;
            }
        }
        return null;
    }

    public static ArrayList<Taxon> taxon(){
        return taxon;
    }

    public static String classify(String word){
        for(Taxon t : taxon){
            for(String item : t.items()){
                if(item.equals(word)){
                    return t.getName();
                }
            }
        }
        return null;
    }

    public static void add(String name, String item){
        Taxon taxon = taxon(name);
        if(taxon != null){
            taxon.add(item);
        }else{
            taxon = new Taxon(name);
            taxon.add(item);
            Andromeda.taxon.add(taxon);
        }
    }

    public static void save(){
        try (FileOutputStream fileOut = new FileOutputStream(Telifie.configDirectory() + "andromeda/taxon.telifie");
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(taxon);
            Log.out(Event.Type.PUT, "TAXON.TELIFIE SAVED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class tools {

        public static String escapeMarkdownForJson(String markdownText) {
            String escapedText = markdownText.replace("\\", "\\\\");
            escapedText = escapedText.replace("\"", "\\\"");
            escapedText = escapedText.replace("\n", "\\n");
            escapedText = escapedText.replace("\r", "\\r");
            escapedText = escapedText.replace("\t", "\\t");
            escapedText = escapedText.replace("\b", "\\b");
            escapedText = escapedText.replace("\f", "\\f");
            return escapedText;
        }

        public static boolean contains(String[] things, String string){
            for (String thing: things) {
                if(string.contains(thing)){
                    return true;
                }
            }
            return false;
        }

        public static int has(String[] things, String string){
            for(int i = 0; i < things.length; i++){
                if(string.contains(things[i])){
                    return i;
                }
            }
            return -1;
        }

        public static String removeWords(String text, String[] wordsToRemove) {
            StringBuilder sb = new StringBuilder();
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (!Arrays.asList(wordsToRemove).contains(word.toLowerCase())) {
                    sb.append(word).append(" ");
                }
            }
            return sb.toString().trim();
        }

        public static String removeWords(String text, Taxon wordsToRemove) {
            StringBuilder sb = new StringBuilder();
            Set<String> toRemoveSet = new HashSet<>(wordsToRemove.items()); // Convert list to a set for efficient lookups.
            String[] words = text.split("\\W+"); // Split on non-word characters to handle punctuation.
            for (String word : words) {
                if (!toRemoveSet.contains(word.toLowerCase().trim())) {
                    sb.append(word).append(" ");
                }
            }
            return sb.toString().trim();
        }

        public static boolean equals(char sample, char[] chars){
            for(char ch : chars){
                if(sample == ch){
                    return true;
                }
            }
            return false;
        }

        public static String escape(String input){
            return new StringEscapeUtils().escapeJson(input);
        }

        public static String htmlEscape(String string){
            return  HtmlEscapers.htmlEscaper().escape(string);
        }

        public static int levenshtein(String x, String y) {
            int[][] dp = new int[x.length() + 1][y.length() + 1];
            for (int i = 0; i <= x.length(); i++) {
                for (int j = 0; j <= y.length(); j++) {
                    if (i == 0) {
                        dp[i][j] = j;
                    } else if (j == 0) {
                        dp[i][j] = i;
                    } else {
                        dp[i][j] = min(
                                dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1);
                    }
                }
            }

            return dp[x.length()][y.length()];
        }

        private static int costOfSubstitution(char a, char b) {
            return a == b ? 0 : 1;
        }

        private static int min(int... numbers) {
            return java.util.Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
        }
    }
}