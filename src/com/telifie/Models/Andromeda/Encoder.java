package com.telifie.Models.Andromeda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Encoder {

    private int embeddingDimension;
    private Map<String, double[]> wordEmbeddings;

    public static List<Unit> tokenize(String text, boolean cleaned){
        List<String> sentences = new ArrayList<>();
        List<Unit> tokenized = new ArrayList<>();
        StringBuilder currentSentence = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            currentSentence.append(c);
            if (Andromeda.tools.equals(c, new char[] {'.', '!', '?', '\n'})) {
                sentences.add(currentSentence.toString().trim());
                currentSentence = new StringBuilder();
            }
        }
        if (!currentSentence.isEmpty()) {
            sentences.add(currentSentence.toString().trim());
        }
        for(String sentence : sentences){
            if(cleaned){
                sentence = clean(sentence);
            }
            tokenized.add(new Unit(sentence));
        }
        return tokenized;
    }

    public static String clean(String text){
        String cleanedText = text.toLowerCase().trim();
        cleanedText = cleanedText.replaceAll("[\\d+]", "");
        cleanedText = Andromeda.tools.removeWords(cleanedText, Andromeda.STOP_WORDS);
        cleanedText = cleanedText.replaceAll("[^a-zA-Z0-9 ]", "");
        return cleanedText;
    }

    public static Map<String, Map<String, Integer>> coOccurrenceMatrix(List<String> tokens, int windowSize) {

        Map<String, Map<String, Integer>> coOccurrenceMatrix = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            String currentWord = tokens.get(i);
            if (!coOccurrenceMatrix.containsKey(currentWord)) {
                coOccurrenceMatrix.put(currentWord, new HashMap<>());
            }
            Map<String, Integer> wordCounts = coOccurrenceMatrix.get(currentWord);
            for (int j = i - windowSize; j <= i + windowSize; j++) {
                if (j != i && j >= 0 && j < tokens.size()) {
                    String contextWord = tokens.get(j);
                    wordCounts.put(contextWord, wordCounts.getOrDefault(contextWord, 0) + 1);
                }
            }
        }
        return coOccurrenceMatrix;
    }

    public static Map<String, Map<String, Double>> preprocessWithPMI(Map<String, Map<String, Integer>> coOccurrenceMatrix) {
        Map<String, Map<String, Double>> pmiMatrix = new HashMap<>();

        long totalCoOccurrences = 0;
        for (Map<String, Integer> contextWords : coOccurrenceMatrix.values()) {
            for (int count : contextWords.values()) {
                totalCoOccurrences += count;
            }
        }

        // Step 2: Calculate PMI for each word pair
        for (String word : coOccurrenceMatrix.keySet()) {
            Map<String, Integer> contextWords = coOccurrenceMatrix.get(word);
            pmiMatrix.put(word, new HashMap<>());

            for (String contextWord : contextWords.keySet()) {
                int coOccurrenceCount = contextWords.get(contextWord);

                // Calculate PMI
                double pmi = Math.log((double) coOccurrenceCount * totalCoOccurrences /
                        (contextWords.values().stream().mapToInt(Integer::intValue).sum() *
                                coOccurrenceMatrix.get(contextWord).values().stream().mapToInt(Integer::intValue).sum()));

                // Store PMI in the matrix
                pmiMatrix.get(word).put(contextWord, pmi);
            }
        }
        return pmiMatrix;
    }


    // Define a loss function based on Mean Squared Error (MSE)
    public static double calculateLoss(Map<String, Map<String, Double>> predictedPMI, Map<String, Map<String, Double>> expectedPMI) {
        double totalLoss = 0.0;
        int totalPairs = 0;

        for (String word : predictedPMI.keySet()) {
            for (String contextWord : predictedPMI.get(word).keySet()) {
                double predicted = predictedPMI.get(word).get(contextWord);
                double expected = expectedPMI.get(word).get(contextWord);

                // Calculate the squared error between predicted and expected PMI
                double squaredError = Math.pow(predicted - expected, 2);

                totalLoss += squaredError;
                totalPairs++;
            }
        }
        // Calculate the mean squared error
        return totalLoss / totalPairs;
    }

    public class SGDWordEmbeddingsOptimizer {

        // Hyperparameter: Learning rate
        private double learningRate;

        public SGDWordEmbeddingsOptimizer(double learningRate) {
            this.learningRate = learningRate;
        }

        // Implement the SGD optimization step
        public void optimizeWordEmbeddings(Map<String, double[]> wordEmbeddings, Map<String, Map<String, Double>> predictedPMI, Map<String, Map<String, Double>> expectedPMI) {
            for (String targetWord : wordEmbeddings.keySet()) {
                double[] targetEmbedding = wordEmbeddings.get(targetWord);

                for (String contextWord : wordEmbeddings.keySet()) {
                    double[] contextEmbedding = wordEmbeddings.get(contextWord);

                    double predicted = predictedPMI.get(targetWord).get(contextWord);
                    double expected = expectedPMI.get(targetWord).get(contextWord);

                    double gradient = 2.0 * (predicted - expected);

                    // Update the target word embedding
                    for (int i = 0; i < targetEmbedding.length; i++) {
                        targetEmbedding[i] -= learningRate * gradient * contextEmbedding[i];
                    }

                    // Update the context word embedding (skip this part if you're not using skip-gram)
                    for (int i = 0; i < contextEmbedding.length; i++) {
                        contextEmbedding[i] -= learningRate * gradient * targetEmbedding[i];
                    }
                }
            }
        }
    }
}