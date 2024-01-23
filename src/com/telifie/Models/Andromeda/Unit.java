package com.telifie.Models.Andromeda;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Unit {

    private final String[] tokens;
    private final String text, cleaned;

    public Unit(String text) {
        this.text = text.trim();
        this.cleaned = Encoder.clean(text);
        this.tokens = Arrays.stream(text.split("\\s+")).filter(token -> !token.isEmpty()).toArray(String[]::new);
    }

    public String[] keywords(int numKeywords) {
        String[] words = cleaned.split("\\s+");
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String word : words) {
            wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
        }
        String[] sortedWords = wordFreq.keySet().toArray(new String[0]);
        Arrays.sort(sortedWords, (a, b) -> wordFreq.get(b) - wordFreq.get(a));
        int numKeywordsToExtract = Math.min(numKeywords, sortedWords.length);
        return Arrays.copyOfRange(sortedWords, 0, numKeywordsToExtract);
    }

    public String text() {
        return this.text;
    }

    public String cleaned() {
        return this.cleaned;
    }

    public String[] tokens() {
        return this.tokens;
    }

    public boolean startsWith(String t){
        String start = this.tokens[0];
        if(t.equals(Andromeda.classify(start))){
            return true;
        }
        return false;
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
