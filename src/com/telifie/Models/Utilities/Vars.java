package com.telifie.Models.Utilities;

public class Vars {
    public static final int PRIVATE = 0, PROTECTED = 1, PUBLIC = 2;

    public enum Languages {

        ENGLISH("ENGLISH"),
        SPANISH("SPANISH"),
        FRENCH("FRENCH"),
        CHINESE("CHINESE"),
        GERMAN("GERMAN");

        private String displayName = "ENGLISH";
        private Languages(String displayName){
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

}
