package com.telifie.Models.Utilities;

import org.bson.Document;
import java.io.Serializable;

public class Theme implements Serializable {

    private String name, font, background, darkBackground, borderColor;
    private int cornerRadius;
    private Color color;
    private boolean alwaysDark = false;

    public Theme(Document document){
        this.name = document.getString("name");
        this.background = document.getString("background");
        this.cornerRadius = document.getInteger("corner_radius");
        this.color = new Color(document.get("color", Document.class));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "{\"name\" : \"" + name + '\"' +
                ", \"background\" : \"" + background + '\"' +
                ", \"corner_radius\" : " + cornerRadius +
                ", \"color\" : " + color +
                '}';
    }

    class Color {

        private String name, color, light;
        private boolean alwaysDark = false;

        public Color(Document document){
            this.name = document.getString("name");
            this.alwaysDark = (document.getBoolean("always_dark") == null ? false : document.getBoolean("always_dark"));
            this.color = document.getString("color");
            this.light = document.getString("light");
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isAlwaysDark() {
            return alwaysDark;
        }

        public void setAlwaysDark(boolean alwaysDark) {
            this.alwaysDark = alwaysDark;
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
            return "{\"name\" : \"" + name + '\"' +
                    ", \"always_dark\" : " + alwaysDark +
                    ", \"color\" : \"" + color + '\"' +
                    ", \"light\" : \"" + light + '\"' +
                    '}';
        }
    }
}
