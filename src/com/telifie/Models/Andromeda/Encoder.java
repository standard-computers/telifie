package com.telifie.Models.Andromeda;

import java.util.ArrayList;
import java.util.List;

public class Encoder {

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
        String ct = text.toLowerCase().trim();
        ct = ct.replaceAll("[\\d+]", "");
        ct = Andromeda.tools.removeWords(ct, Andromeda.taxon("stop_words"));
        ct = ct.replaceAll("[^a-zA-Z0-9 ]", "");
        return ct;
    }
}