package com.telifie.Models.Utilities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Files {

    private static String name;

    public Files(String name){
        this.name = name;
    }

    public static class csv {

        public static void append(String line) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Telifie.configDirectory() + "/" + name, true))) {
                writer.write(line);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
