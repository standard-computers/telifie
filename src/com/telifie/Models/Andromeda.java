package com.telifie.Models;

import com.mongodb.client.*;
import com.telifie.Models.Clients.Client;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Andromeda extends Client{

    private static ArrayList<taxon> taxon = new ArrayList<>();
    protected static Configuration config;

    public Andromeda(Configuration config, boolean index){
        super(config);
        this.config = config;
        super.collection = "taxon";
        ArrayList<Document> documents = super.find(new Document());
        documents.forEach(document -> taxon.add(new taxon(document)));
        if(index){
            index();
        }
    }

    public Andromeda(Configuration config){
        this(config, false);
    }

    private void index(){
        try (MongoClient mongoClient = MongoClients.create(config.getDomain().getUri())) {
            MongoDatabase database = mongoClient.getDatabase(config.getDomain().getAlt());
            MongoCollection<Document> collection = database.getCollection("articles");
            MongoCursor<Document> cursor = collection.find().iterator();
            int totalCount = (int) collection.countDocuments();
            int processedCount = 0;
            while (cursor.hasNext()) {
                Document document = cursor.next();
                String title = (document.getString("title") == null ? null : document.getString("title").toLowerCase());
                String description = (document.getString("description") == null ? null : document.getString("description").toLowerCase());
                if(title != null && description != null){
                    add(description, title);
                }
                processedCount++;
                System.out.println((processedCount / totalCount) + "% indexed (" + processedCount + " / " + totalCount + ")");
            }
            cursor.close();
        }catch (Exception e){
            Telifie.console.out.string(e.toString());
        }
    }

    public static class taxon extends Client{

        private String name;
        private ArrayList<String> items = new ArrayList<>();

        public taxon(String name) {
            super(Andromeda.config);
            super.collection = "taxon";
            this.name = name.toLowerCase().trim();
        }

        public taxon(Document document){
            super(Andromeda.config);
            super.collection = "taxon";
            this.name = document.getString("name");
            this.items = document.get("items", ArrayList.class);
        }

        public ArrayList<String> items(){
            return items;
        }

        public void add(String string){
            items.add(string);
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

        public unit(String unit) {
            this.tokens = Arrays.stream(unit.split("\\s+")).filter(token -> !token.isEmpty()).toArray(String[]::new);
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

        private static List<String> sentences;

        public static List<unit> tokenize(String text, boolean cleaned){
            sentences = new ArrayList<>();
            List<Andromeda.unit> tokenized = new ArrayList<>();
            StringBuilder currentSentence = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                currentSentence.append(c);
                if (Telifie.tools.strings.equals(c, new char[] {'.', '!', '?'})) {
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
            cleanedText = Telifie.tools.strings.removeWords(cleanedText, Telifie.stopWords);
            cleanedText = cleanedText.replaceAll("[^a-zA-Z0-9 ]", "");
            return cleanedText;
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