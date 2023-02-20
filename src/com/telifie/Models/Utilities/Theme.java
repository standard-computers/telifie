package com.telifie.Models.Utilities;

import org.bson.Document;
import org.json.JSONObject;

import java.io.Serializable;

public class Theme implements Serializable {

    class Color {

        private String name, color, light;

        public Color(String name, String color, String light) {
            this.name = name;
            this.color = color;
            this.light = light;
        }

        public Color(Document document){
            this.name = document.getString("name");
            this.color = document.getString("color");
            this.light = document.getString("light");
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getLight() {
            return light;
        }

        public void setLight(String light) {
            this.light = light;
        }

        @Override
        public String toString() {
            return "{" +
                    "\"name\" : \"" + name + '\"' +
                    ", \"color\" : \"" + color + '\"' +
                    ", \"light\" : \"" + light + '\"' +
                    '}';
        }

        public JSONObject toJson(){
            return new JSONObject(this.toString());
        }

    }

    private String name, background, cornerRadius;
    private Color color;

    public Theme(Document document){
        this.name = document.getString("name");
        this.background = document.getString("background");
        this.cornerRadius = document.getString("corner_radius");
        this.color = new Color(document.get("color", Document.class));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(String cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "{" +
                "\"name\" : \"" + name + '\"' +
                ", \"background\" : \"" + background + '\"' +
                ", \"corner_radius\" : \"" + cornerRadius + '\"' +
                ", \"color\" : " + color +
                '}';
    }

    public JSONObject toJson(){
        return new JSONObject(this.toString());
    }

}
