package com.telifie.Models.Utilities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public class Log {

    public static void out(Event.Type event, String message, String code){
        System.out.println("[" + readableTimeDate() + "] " + event + " _: " + code + " >> " + message);
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