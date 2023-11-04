package com.telifie.Models.Andromeda;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Taxon implements Serializable {

    private final String name;
    private ArrayList<String> items = new ArrayList<>();

    public Taxon(String name) {
        this.name = name.toLowerCase().trim();
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> items(){
        return items;
    }

    public void add(String string){
        for(String item : items){
            if(item.equals(string)){
                return;
            }
        }
        items.add(string);
    }

    @Override
    public String toString() {
        return "{\"name\" : \"" + name + "\"" +
                ", \"items\" : " + items.stream().map(tag -> "\"" + tag + "\"").collect(Collectors.joining(", ", "[", "]")) +
                '}';
    }
}
