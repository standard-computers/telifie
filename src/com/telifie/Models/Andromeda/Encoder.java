package com.telifie.Models.Andromeda;

import com.telifie.Models.Utilities.Console;

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
        String cleanedText = text.toLowerCase().trim();
        cleanedText = cleanedText.replaceAll("[\\d+]", "");
        cleanedText = Andromeda.tools.removeWords(cleanedText, Andromeda.taxon("stop_words"));
        cleanedText = cleanedText.replaceAll("[^a-zA-Z0-9 ]", "");
        return cleanedText;
    }
}