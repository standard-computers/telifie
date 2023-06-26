package com.telifie.Models.Utilities;

import org.bson.Document;
import java.io.Serializable;

public class Theme implements Serializable {

    private String name;
    private final String background;
    private final int cornerRadius;
    private final Color color;

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

    static class Color {

        private String name;
        private final String color;
        private final String light;
        private final boolean alwaysDark;

        public Color(Document document){
            this.name = document.getString("name");
            this.alwaysDark = (document.getBoolean("always_dark") != null && document.getBoolean("always_dark"));
            this.color = document.getString("color");
            this.light = document.getString("light");
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
                    ", \"always_dark\" : " + alwaysDark +
                    ", \"color\" : \"" + color + '\"' +
                    ", \"light\" : \"" + light + '\"' +
                    '}';
        }
    }
}
