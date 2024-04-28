package com.telifie.Models.Utilities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Timer;
import java.util.TimerTask;

public class Log {

    public static void out(Event.Type event, String message, String code){
        System.out.println("[" + readableTimeDate() + "] " + code + " >> " + message);
        appendToCSV(event, message, code);
    }

    public static void error(String message, String code){
        out(Event.Type.ERROR, message, code);
    }

    public static void flag(String message, String code){
        out(Event.Type.FLAG, message, code);
    }

    public static void put(String message, String code){
        out(Event.Type.PUT, message, code);
    }

    public static void message(String message, String code){
        out(Event.Type.MESSAGE, message, code);
    }

    public static void console(String message){
        System.out.println("[" + readableTimeDate() + "] " + message);
    }

    public static void wrap(String text, int width) {
        StringBuilder sb = new StringBuilder();
        final int[] index = {0};
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (index[0] < text.length()) {
                    int endIndex = Math.min(index[0] + width, text.length());
                    String line = text.substring(index[0], endIndex);
                    int lastSpace = line.lastIndexOf(' ');
                    if (lastSpace != -1 && endIndex != text.length()) {
                        line = line.substring(0, lastSpace);
                        index[0] += lastSpace + 1;
                    } else {
                        index[0] += width;
                    }
                    sb.append(line).append('\n');
                    System.out.print(sb.toString());
                    sb.setLength(0);
                } else {
                    timer.cancel();
                }
            }
        };
        timer.scheduleAtFixedRate(task, 0, 50);
    }

    private static void appendToCSV(Event.Type event, String message, String code) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Telifie.configDirectory() + "/telifie_log.csv", true))) {
            writer.write(event.toString() + "," + message + "," + Telifie.epochTime() + "," + readableTimeDate() + ", " + code);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readableTimeDate(){
        long epochTimestamp = Telifie.epochTime();
        Instant instant = Instant.ofEpochSecond(epochTimestamp);
        ZoneId zoneId = ZoneId.of("UTC");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);
        return formatter.format(instant);
    }
}