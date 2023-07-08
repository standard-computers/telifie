package com.telifie.Models;

import com.mongodb.client.*;
import com.telifie.Models.Clients.Client;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Andromeda extends Client{

    private static ArrayList<taxon> taxon = new ArrayList<>();
    private static List<String> sentences;
    private static List<unit> tokens = new ArrayList<>();
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
            vectorizer vectorizer = new vectorizer(100, 2, 0.01, 10);
            while (cursor.hasNext()) {
                Article a = new Article(cursor.next());
                String title = (a.getTitle() == null ? null : a.getTitle().toLowerCase());
                String description = (a.getDescription() == null ? null : a.getDescription());
                String content = (a.getContent() == null ? null : a.getContent());
                if(title != null && description != null){
                    add(description, title);
                }
                if(content != null){
                    Andromeda.encoder.tokenize(a.getContent(), true).forEach(s -> {
                        tokens.add(s);
                        vectorizer.train(s.tokens);
                    });
                }
                processedCount++;
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

    protected static class vectorizer {
        private Map<String, double[]> wordVectors;
        private Map<String, Integer> wordFreq;
        private int vectorSize;
        private int windowSize;
        private double learningRate;
        private int epochs;

        public vectorizer(int vectorSize, int windowSize, double learningRate, int epochs) {
            this.vectorSize = vectorSize;
            this.windowSize = windowSize;
            this.learningRate = learningRate;
            this.epochs = epochs;
            this.wordVectors = new HashMap<>();
            this.wordFreq = new HashMap<>();
        }

        public void train(String[] tokens) {
            for (String token : tokens) {
                wordFreq.put(token, wordFreq.getOrDefault(token, 0) + 1);
            }

            int numWords = wordFreq.size();
            int[][] coocurrenceMatrix = new int[numWords][numWords];

            for (int i = 0; i < tokens.length; i++) {
                int centerIdx = i;
                String centerWord = tokens[centerIdx];

                for (int j = Math.max(0, centerIdx - windowSize); j < Math.min(tokens.length, centerIdx + windowSize + 1); j++) {
                    if (j == centerIdx) {
                        continue;
                    }
                    String contextWord = tokens[j];
                    coocurrenceMatrix[getIndex(centerWord)][getIndex(contextWord)]++;
                }
            }

            initializeWordVectors(numWords);

            for (int epoch = 0; epoch < epochs; epoch++) {
                for (int i = 0; i < tokens.length; i++) {
                    int centerIdx = i;
                    String centerWord = tokens[centerIdx];

                    for (int j = Math.max(0, centerIdx - windowSize); j < Math.min(tokens.length, centerIdx + windowSize + 1); j++) {
                        if (j == centerIdx) {
                            continue;
                        }
                        String contextWord = tokens[j];
                        updateWordVectors(centerWord, contextWord, coocurrenceMatrix);
                    }
                }
            }
        }

        private void initializeWordVectors(int numWords) {
            for (String word : wordFreq.keySet()) {
                double[] vector = new double[vectorSize];
                for (int i = 0; i < vectorSize; i++) {
                    vector[i] = Math.random();
                }
                wordVectors.put(word, vector);
            }
        }

        private void updateWordVectors(String centerWord, String contextWord, int[][] coocurrenceMatrix) {
            double[] centerVector = wordVectors.get(centerWord);
            double[] contextVector = wordVectors.get(contextWord);

            for (int i = 0; i < vectorSize; i++) {
                double gradient = 0.0;
                for (int j = 0; j < vectorSize; j++) {
                    gradient += centerVector[j] * contextVector[j];
                }

                gradient -= coocurrenceMatrix[getIndex(centerWord)][getIndex(contextWord)];

                for (int j = 0; j < vectorSize; j++) {
                    centerVector[j] -= learningRate * gradient * contextVector[j];
                    contextVector[j] -= learningRate * gradient * centerVector[j];
                }
            }
        }

        public double[] getWordVector(String word) {
            return wordVectors.get(word);
        }

        private int getIndex(String word) {
            int index = 0;
            for (String key : wordFreq.keySet()) {
                if (key.equals(word)) {
                    return index;
                }
                index++;
            }
            return -1; // Word not found in the wordFreq map
        }

        public void exportWordEmbeddings(String filePath) {
            try (FileWriter writer = new FileWriter(filePath)) {
                for (String word : wordVectors.keySet()) {
                    double[] vector = wordVectors.get(word);
                    writer.write(word + " ");
                    for (double value : vector) {
                        writer.write(value + " ");
                    }
                    writer.write("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}