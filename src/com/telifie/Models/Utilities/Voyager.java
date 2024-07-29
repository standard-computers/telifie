package com.telifie.Models.Utilities;

public class Voyager {

    public Voyager(){
    }

    public static class Unit {

        private String text;

        public Unit(String text){
            this.text = text;
        }

        public boolean isInterrogative(){
            String[] interrogativeWords = {"who", "what", "when", "where", "why", "how", "which", "whom", "whose", "whos"};
            for(String word : interrogativeWords){
                if(text.startsWith(word)){
                    return true;
                }
            }
            return false;
        }
    }
}