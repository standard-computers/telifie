package com.telifie.Models.Utilities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public class Log {

    public static void out(Event.Type event, String message){
        System.out.println(message);
        appendToCSV(event, message);
    }

    public static void error(String message){
        out(Event.Type.ERROR, message);
    }

    public static void message(String message){
        out(Event.Type.MESSAGE, message);
    }

    private static void appendToCSV(Event.Type event, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Telifie.configDirectory() + "/telifie_log.csv", true))) {
            String dataToAppend = event.toString() + "," + message + "," + Telifie.epochTime() + "," + readableTimeDate();
            writer.write(dataToAppend);
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