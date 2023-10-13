package com.telifie.Models;

import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.telifie.Models.Clients.Client;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Andromeda extends Client{

    private static ArrayList<taxon> taxon = new ArrayList<>();
    private static List<String> sentences;
    private static List<unit> tokens = new ArrayList<>();
    protected static Configuration config;
    protected static Session session;

    public Andromeda(Configuration config, Session session, boolean index){
        super(session);
        this.config = config;
        super.collection = "taxon";
        ArrayList<Document> documents = super.find(new Document());
        documents.forEach(document -> taxon.add(new taxon(document)));
        if(index){
            index();
        }
    }

    public Andromeda(Configuration config, Session session){
        this(config, session, false);
    }

    private void index(){
        try {
            MongoDatabase database = super.mc.getDatabase(session.getDomain());
            MongoCollection<Document> collection = database.getCollection("articles");
            MongoCursor<Document> cursor = collection.find().iterator();
            while (cursor.hasNext()) {
                Article a = new Article(cursor.next());
                String title = (a.getTitle() == null ? null : a.getTitle().toLowerCase());
                String description = (a.getDescription() == null ? null : a.getDescription());
                String content = (a.getContent() == null ? null : a.getContent());
                if(title != null && description != null){
                    add(description, title);
                }
                if(content != null){
                    Andromeda.encoder.tokenize(a.getContent(), true).forEach(s -> tokens.add(s));
                }
            }
            cursor.close();
        }catch (Exception e){
            System.out.println(e);
        }
    }

    public static class taxon extends Client{

        private String name;
        private ArrayList<String> items = new ArrayList<>();

        public taxon(String name) {
            super(Andromeda.session);
            super.collection = "taxon";
            this.name = name.toLowerCase().trim();
        }

        public taxon(Document document){
            super(Andromeda.session);
            super.collection = "taxon";
            this.name = document.getString("name");
            this.items = document.get("items", ArrayList.class);
            System.out.println(name);
            System.out.println(items.toString());
        }

        public ArrayList<String> items(){
            return items;
        }

        public void add(String string){
            for(String item : items){
                if(item.equals(string)){
                    return;
                }
            }
            items.add(string);
            super.updateOne(
                    new Document("name", name),
                    new Document("$push", new Document("items", string)),
                    new UpdateOptions().upsert(true)
            );
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "{\"name\" : \"" + name + "\"" +
                    ", \"items\" : " + items.stream().map(tag -> "\"" + tag + "\"").collect(Collectors.joining(", ", "[", "]")) +
                    '}';
        }
    }

    public static taxon taxon(String name) {
        for (taxon taxon : taxon) {
            if (taxon.name.equals(name.toLowerCase().trim())) {
                return taxon;
            }
        }
        return null;
    }

    public static String classify(String word){
        for(taxon t : taxon){
            for(String item : t.items()){
                if(item.equals(word)){
                    return t.getName();
                }
            }
        }
        return null;
    }

    public static void add(String name, String item){
        taxon taxon = taxon(name);
        if(taxon != null){
            taxon.add(item);
        }else{
            taxon = new taxon(name);
            taxon.add(item);
            Andromeda.taxon.add(taxon);
        }
    }

    public static class unit {

        private final String[] tokens;
        private final String text;

        public unit(String unit) {
            this.text = unit;
            this.tokens = Arrays.stream(unit.split("\\s+")).filter(token -> !token.isEmpty()).toArray(String[]::new);
        }

        public static String correct(String text) {
            Language language = new AmericanEnglish();
            JLanguageTool langTool = new JLanguageTool(language);
            List<RuleMatch> matches;
            try {
                matches = langTool.check(text);
                for (int i = matches.size() - 1; i >= 0; i--) {
                    RuleMatch match = matches.get(i);
                    String replacement = match.getSuggestedReplacements().get(0);
                    text = text.substring(0, match.getFromPos()) + replacement + text.substring(match.getToPos());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return text;
        }

        public String[] keywords(int numKeywords) {
            String wt = Andromeda.encoder.clean(this.text.replaceAll("[^a-zA-Z ]", "").toLowerCase());
            String[] words = wt.split("\\s+");
            Map<String, Integer> wordFreq = new HashMap<>();
            for (String word : words) {
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
            String[] sortedWords = wordFreq.keySet().toArray(new String[0]);
            Arrays.sort(sortedWords, (a, b) -> wordFreq.get(b) - wordFreq.get(a));
            int numKeywordsToExtract = Math.min(numKeywords, sortedWords.length);
            String[] keywords = Arrays.copyOfRange(sortedWords, 0, numKeywordsToExtract);
            return keywords;
        }

        public String[] tokens() {
            return tokens;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < tokens.length; i++) {
                builder.append(tokens[i]);
                if (i < tokens.length - 1) {
                    builder.append(", ");
                }
            }
            builder.append("]");
            return builder.toString();
        }
    }

    public static class encoder {

        public static List<unit> tokenize(String text, boolean cleaned){
            sentences = new ArrayList<>();
            List<Andromeda.unit> tokenized = new ArrayList<>();
            StringBuilder currentSentence = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                currentSentence.append(c);
                if (Telifie.tools.equals(c, new char[] {'.', '!', '?'})) {
                    sentences.add(currentSentence.toString().trim());
                    currentSentence = new StringBuilder();
                }
            }
            if (currentSentence.length() > 0) {
                sentences.add(currentSentence.toString().trim());
            }
            for(String sentence : sentences){
                if(cleaned){
                    sentence = clean(sentence);
                }
                System.out.println(new Andromeda.unit(sentence));
                tokenized.add(new Andromeda.unit(sentence));
            }
            return tokenized;
        }

        public static String clean(String text){
            String cleanedText = text.toLowerCase().trim();
            cleanedText = cleanedText.replaceAll("[\\d+]", "");
            cleanedText = Telifie.tools.removeWords(cleanedText, Telifie.stopWords);
            cleanedText = cleanedText.replaceAll("[^a-zA-Z0-9 ]", "");
            return cleanedText;
        }

        public static String[] nova(String text){
            return text.split("\\s+");
        }

    }

    public static class decoder {

    }

    public static double[] softmax(double[] inputs) {
        double max = Arrays.stream(inputs).max().orElse(0.0);
        double sum = 0.0;
        double[] softmaxOutputs = new double[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            softmaxOutputs[i] = Math.exp(inputs[i] - max);
            sum += softmaxOutputs[i];
        }
        for (int i = 0; i < softmaxOutputs.length; i++) {
            softmaxOutputs[i] /= sum;
        }
        return softmaxOutputs;
    }
}