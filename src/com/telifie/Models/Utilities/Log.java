package com.telifie.Models.Utilities;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public class Log {

    public static String logFile = Telifie.configDirectory() + "/telifie_log.csv";

    public static void out(Event.Type event, String message){
        System.out.println(message);
        try {
            FileWriter fileWriter = new FileWriter(Log.logFile, true);
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            String[] dataToAppend = {event.toString(), message, Integer.toString(Telifie.epochTime()), readableTimeDate()};
            csvWriter.writeNext(dataToAppend);
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void error(String message){
        System.err.println(message);
        try {
            FileWriter fileWriter = new FileWriter(Log.logFile, true);
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            String[] dataToAppend = {Event.Type.ERROR.toString(), message, Integer.toString(Telifie.epochTime())};
            csvWriter.writeNext(dataToAppend);
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readableTimeDate(){
        long epochTimestamp = Telifie.epochTime();
        Instant instant = Instant.ofEpochSecond(epochTimestamp);
        ZoneId zoneId = ZoneId.of("UTC");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);
        return formatter.format(instant);
    }
}