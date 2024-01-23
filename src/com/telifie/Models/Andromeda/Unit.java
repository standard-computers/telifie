package com.telifie.Models.Andromeda;

import java.util.*;

public class Unit {

    private final String[] tokens, cleanedTokens;
    private final String text, cleaned;

    public Unit(String text) {
        this.text = text.trim();
        this.cleaned = clean(text);
        this.tokens = Arrays.stream(text.split("\\s+")).filter(token -> !token.isEmpty()).toArray(String[]::new);
        this.cleanedTokens = Arrays.stream(cleaned.split("\\s+")).filter(token -> !token.isEmpty()).toArray(String[]::new);
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

    public String[] cleanedTokens() {
        return this.cleanedTokens;
    }

    public String clean(String text){
        String ct = remove(Andromeda.taxon("stop_words"));
        ct = ct.replaceAll("[^a-zA-Z0-9 ]", "");
        return ct;
    }

    public boolean startsWith(Taxon t){
        for(String s : t.items()){
            if(text.startsWith(s)){
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if Unit text contains any words part of the identified taxon.
     * @param t Taxon of words to search for
     * @return
     */
    public boolean contains(Taxon t){
        for(String s : t.items()){
            if(text.contains(s)){
                return true;
            }
        }
        return false;
    }

    public String remove(Taxon t){
        StringBuilder sb = new StringBuilder();
        Set<String> toRemoveSet = new HashSet<>(t.items());
        String[] words = text.split("\\W+");
        for (String word : words) {
            if (!toRemoveSet.contains(word.toLowerCase().trim())) {
                sb.append(word).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public String get(Taxon t){
        for(String s : t.items()){
            if(text.contains(s)){
                return s;
            }
        }
        return "";
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
