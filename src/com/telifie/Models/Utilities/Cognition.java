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

    public Cognition(){}

    public Cognition(String corpusPath){
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

    public Cognition(boolean fromDomain){ //Ini from db with new name
        Log.console("Starting build for *.tlfi.knwldg :)");
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
                    embeddings[i][j] = random.nextDouble() - 0.5;
                }
            }
        }

        public double[] getEmbedding(int index) {
            return embeddings[index];
        }
    }

    public class FeedForwardNetwork {
        private LinearProjection layer1, layer2;

        public FeedForwardNetwork(int modelDim, int hiddenDim) {
            layer1 = new LinearProjection(modelDim, hiddenDim);
            layer2 = new LinearProjection(hiddenDim, modelDim);
        }

        public double[][] forward(double[][] input) {
            double[][] hidden = layer1.project(input);
//            hidden = activate(hidden); // Apply activation function
            return layer2.project(hidden);
        }
    }


    public class MultiHeadAttention {
        private int numHeads;
        private int headDim;
        private LinearProjection queryProjection, keyProjection, valueProjection, outputProjection;

        public MultiHeadAttention(int numHeads, int modelDim) {
            this.numHeads = numHeads;
            this.headDim = modelDim / numHeads;
            queryProjection = new LinearProjection(modelDim, modelDim);
            keyProjection = new LinearProjection(modelDim, modelDim);
            valueProjection = new LinearProjection(modelDim, modelDim);
            outputProjection = new LinearProjection(modelDim, modelDim);
        }

        public double[][] computeAttention(double[][] queries, double[][] keys, double[][] values) {
            double[][] projectedQueries = queryProjection.project(queries);
            double[][] projectedKeys = keyProjection.project(keys);
            double[][] projectedValues = valueProjection.project(values);

            // Split into heads and compute attention scores
            // ...

            // Concatenate heads and project output
//            double[][] output = outputProjection.project(concatenatedHeads);
//            return output;
            return null;
        }
    }

    public class LinearProjection {
        private double[][] weights;
        private double[] biases;
        private int inputDim;
        private int outputDim;

        public LinearProjection(int inputDim, int outputDim) {
            this.inputDim = inputDim;
            this.outputDim = outputDim;
            this.weights = new double[inputDim][outputDim];
            this.biases = new double[outputDim];
            initializeWeightsAndBiases();
        }

        private void initializeWeightsAndBiases() {
            Random random = new Random();
            for (int i = 0; i < inputDim; i++) {
                for (int j = 0; j < outputDim; j++) {
                    weights[i][j] = random.nextGaussian() * 0.01;
                }
            }
            for (int i = 0; i < outputDim; i++) {
                biases[i] = 0;
            }
        }

        public double[] project(double[] input) {
            double[] output = new double[outputDim];
            for (int j = 0; j < outputDim; j++) {
                output[j] = biases[j];
                for (int i = 0; i < inputDim; i++) {
                    output[j] += input[i] * weights[i][j];
                }
            }
            return output;
        }

        public double[][] project(double[][] input) {
            int batchSize = input.length;
            double[][] output = new double[batchSize][outputDim];
            for (int b = 0; b < batchSize; b++) {
                output[b] = project(input[b]);
            }
            return output;
        }
    }
}