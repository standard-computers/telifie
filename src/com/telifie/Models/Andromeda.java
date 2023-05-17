package com.telifie.Models;

import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class Andromeda {

    private static ArrayList<Andromeda.taxon> taxon = new ArrayList<>();

    public static class taxon {

        private String name, parent;
        private String[] children;
        private String[] items;

        public taxon(String name) {
            this.name = name.toLowerCase().trim();
        }

        public taxon(Document document){
            this.name = document.getString("name");
            this.parent = document.getString("parent");
            this.children = document.getString("children").split(",");
            this.items = document.getString("items").split(",");
        }

        public String get(int index){
            return items[index];
        }

        public String[] getItems(){
            return items;
        }

        public void add(String string){
            String[] newItems = new String[items.length + 1];
            for(int i = 0; i < items.length; i++){
                newItems[i] = items[i];
            }
            newItems[items.length] = string.toLowerCase().trim();
            items = newItems;
        }

        public void remove(String string){
            String[] newItems = new String[items.length - 1];
            int index = 0;
            for(int i = 0; i < items.length; i++){
                if(!items[i].equals(string.toLowerCase().trim())){
                    newItems[index] = items[i];
                    index++;
                }
            }
            items = newItems;
        }

        @Override
        public String toString() {
            return "{\"name\" : \"" + name + "\"" +
                    ", \"parent\" : \"" + parent + "\"" +
                    ", \"children\" : " + Arrays.toString(children) +
                    ", \"items\" : " + Arrays.toString(items) +
                    '}';
        }

        public static boolean exists(String name){
            for(taxon taxon : taxon){
                if(taxon.name.equals(name.toLowerCase().trim())){
                    return true;
                }
            }
            return false;
        }
    }

    public static class unit {

        private final String[] tokens;

        public unit(String unit) {
            this.tokens = Arrays.stream(unit.split("\\s+")).filter(token -> !token.isEmpty()).toArray(String[]::new);
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
}
