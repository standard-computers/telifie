package com.telifie.Models.Utilities;

import com.telifie.Models.Clients.Articles;
import com.telifie.Models.Clients.Domains;
import com.telifie.Models.Domain;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cognition {


    public Cognition(){ //load from model in config
    }

    public Cognition(String name, String corpusPath){
        Log.console("Attempting file read...");
        File cp = new File(corpusPath);
        if(cp.exists()){
            try {
                String content = new String(Files.readAllBytes(Paths.get(corpusPath)));
                ingest(content);
            } catch (IOException e) {
                System.exit(420);
            }
        }else{
            Log.console("File doesn't exist! Try again.");
        }
    }

    public Cognition(String name){ //Ini from db with new name
        Log.console("Starting build for " + name + ".tlfi.knwldg :)");
        String targetDomain = Console.in("Select Domain -> ");
        Domains domains = new Domains();
        Domain downloads = domains.withAlias(targetDomain);
        if(downloads != null){
            Articles articles = new Articles(new Session("telifie@terminal", "telifie"), "articles");

        }else{
            Log.console("Domain doesn't exist! Try again.");
        }
    }

    private void ingest(String content){
        List<String> tokens = tokenize(content);
        Vocabulary v = new Vocabulary(tokens);
        int embeddingSize = 50;
        EmbeddingLayer embeddingLayer = new EmbeddingLayer(v.getVocabSize(), embeddingSize);
        List<double[]> tokenEmbeddings = new ArrayList<>(); //Starting embedding
        for (String token : tokens) {
            int index = v.getIndex(token);
            if (index != -1) {
                double[] embedding = embeddingLayer.getEmbedding(index);
                tokenEmbeddings.add(embedding);
            }
        }

    }

    private List<String> tokenize(String t){
        Log.console("Running tokenizer...");
        List<String> tokens = new ArrayList<>();
        String regex = "\\w+|\\p{Punct}|\\s";
        Matcher matcher = Pattern.compile(regex).matcher(t);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        Log.console("Finished " + tokens.size() + " tokens");
        return tokens;
    }

    public static double[] softmax(double[] input) {
        double[] exp = new double[input.length];
        double sum = 0;
        for (int i = 0; i < input.length; i++) {
            exp[i] = Math.exp(input[i]);
            sum += exp[i];
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = exp[i] / sum;
        }
        return output;
    }

    private class Vocabulary {
        private Map<String, Integer> tokenToIndex;
        private List<String> indexToToken;
        private int vocabSize;

        public Vocabulary(List<String> tokens) {
            tokenToIndex = new HashMap<>();
            indexToToken = new ArrayList<>();
            buildVocabulary(tokens);
        }

        private void buildVocabulary(List<String> tokens) {
            for (String token : tokens) {
                if (!tokenToIndex.containsKey(token)) {
                    tokenToIndex.put(token, indexToToken.size());
                    indexToToken.add(token);
                }
            }
            vocabSize = indexToToken.size();
        }

        public int getIndex(String token) {
            return tokenToIndex.getOrDefault(token, -1); // Returns -1 if the token is not in the vocabulary
        }

        public String getToken(int index) {
            return indexToToken.get(index);
        }

        public int getVocabSize() {
            return vocabSize;
        }
    }

    public class EmbeddingLayer {
        private double[][] embeddings;
        private int embeddingSize;

        public EmbeddingLayer(int vocabSize, int embeddingSize) {
            this.embeddingSize = embeddingSize;
            embeddings = new double[vocabSize][embeddingSize];
            initializeEmbeddings();
        }

        private void initializeEmbeddings() {
            Random random = new Random();
            for (int i = 0; i < embeddings.length; i++) {
                for (int j = 0; j < embeddingSize; j++) {
                    embeddings[i][j] = random.nextDouble() - 0.5; // Random initialization between -0.5 and 0.5
                }
            }
        }

        public double[] getEmbedding(int index) {
            return embeddings[index];
        }
    }
}