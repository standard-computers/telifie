package com.telifie.Models.Andromeda;

import com.telifie.Models.Utilities.*;
import org.apache.commons.text.StringEscapeUtils;
import java.io.*;
import java.util.*;

public class Andromeda {

    private static ArrayList<Taxon> taxon = new ArrayList<>();
    public static String[] STOP_WORDS = new String[]{"a", "an", "and", "are", "as", "at", "or", "make", "be", "by", "for", "from", "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "with", "who", "what", "when", "where", "why", "how", "you"};
    public static final String[] ALPHAS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    public static final String[] NUMERALS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    public static final String[] PROXIMITY = {"near", "nearby", "close to", "around", "within", "in the vicinity of", "within walking distance of", "adjacent to", "bordering", "neighboring", "local to", "surrounding", "not far from", "just off"};

    public Andromeda(){
        Log.out(Event.Type.MESSAGE, "INITIALIZING ANDROMEDA");
        if(new File(Telifie.configDirectory() + "andromeda/taxon.telifie").exists()){
            try (FileInputStream fileIn = new FileInputStream(Telifie.configDirectory() + "andromeda/taxon.telifie");
                 ObjectInputStream in = new ObjectInputStream(fileIn)) {
                this.taxon = (ArrayList<Taxon>) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public Taxon taxon(String name) {
        for (Taxon t : taxon) {
            if (t.getName().equals(name.toLowerCase().trim())) {
                return t;
            }
        }
        return null;
    }

    public ArrayList<Taxon> taxon(){
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

    public void add(String name, String item){
        Taxon taxon = taxon(name);
        if(taxon != null){
            taxon.add(item);
        }else{
            taxon = new Taxon(name);
            taxon.add(item);
            Andromeda.taxon.add(taxon);
        }
        save();
    }

    private void save(){
        try (FileOutputStream fileOut = new FileOutputStream(Telifie.configDirectory() + "andromeda/taxon.telifie");
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(taxon);
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

        public static boolean equals(char sample, char[] chars){
            for(char ch : chars){
                if(sample == ch){
                    return true;
                }
            }
            return false;
        }

        public static String escape(String string){
            return  StringEscapeUtils.escapeJson(string);
        }

        public static String htmlEscape(String string){
            return  StringEscapeUtils.escapeHtml4(string);
        }
    }
}