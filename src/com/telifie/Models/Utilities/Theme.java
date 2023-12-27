package com.telifie.Models.Utilities;

import org.bson.Document;
import java.io.Serializable;

public class Theme implements Serializable {

    private final String name;
    private final String background, style, font;
    private final int cornerRadius, fontSize, scheme;
    private final Color color, foreground;


    public Theme(Document document){
        this.name = document.getString("name");
        this.background = document.getString("background");
        this.style = document.getString("style");
        this.font = (document.getString("font") == null ? "Inter" : document.getString("font"));
        this.cornerRadius = document.getInteger("corner_radius");
        this.fontSize = (document.getInteger("font_size") == null ? 15 : document.getInteger("font_size"));
        this.scheme = (document.getInteger("scheme") == null ? 0 : document.getInteger("scheme")); // 0 = System Scheme, 1 = Light, 2 = Dark
        this.color = new Color(document.get("color", Document.class));
        this.foreground = (document.get("foreground", Document.class) == null ? null : new Color(document.get("foreground", Document.class)));
    }

    @Override
    public String toString() {
        return "{\"name\" : \"" + name + '\"' +
                ", \"background\" : \"" + background + '\"' +
                ", \"style\" : \"" + style + '\"' +
                ", \"font\" : \"" + font + '\"' +
                ", \"corner_radius\" : " + cornerRadius +
                ", \"font_size\" : " + fontSize +
                ", \"scheme\" : " + scheme +
                ", \"color\" : " + color +
                ", \"foreground\" : " + foreground +
                '}';
    }

    class Color {

        private final String name;
        private final String color;
        private final String light;

        public Color(Document document){
            this.name = document.getString("name");
            this.color = document.getString("color");
            this.light = document.getString("light");
        }

        @Override
        public String toString() {
            return "{\"name\" : \"" + name + "\", \"color\" : \"" + color + "\", \"light\" : \"" + light + "\"}";
        }
    }
}