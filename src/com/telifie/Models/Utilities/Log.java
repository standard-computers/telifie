package com.telifie.Models.Utilities;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Log {

    public static String logFile = Telifie.configDirectory() + "/telifie_log.csv";

    public static void out(Event.Type event, String message){
        try {
            FileWriter fileWriter = new FileWriter(Log.logFile, true);
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            String[] dataToAppend = {event.toString(), message, Integer.toString(Telifie.epochTime())};
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
}