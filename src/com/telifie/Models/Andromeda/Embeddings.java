package com.telifie.Models.Andromeda;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Embeddings implements Serializable {

    private int embeddingDimension;
    private Map<String, double[]> wordEmbeddings;

    public Embeddings(int embeddingDimension){
        this.embeddingDimension = embeddingDimension;
        this.wordEmbeddings = new HashMap<>();
    }

    // Initialize word embeddings with random values
    public void initializeWordEmbeddings(List<String> vocabulary) {
        for (String word : vocabulary) {
            double[] embedding = new double[embeddingDimension];
            for (int i = 0; i < embeddingDimension; i++) {
                // Initialize with random values or zeros
                embedding[i] = Math.random(); // You can customize the initialization method
            }
            wordEmbeddings.put(word, embedding);
        }
    }

    public Map<String, double[]> getWordEmbeddings() {
        return wordEmbeddings;
    }
}
