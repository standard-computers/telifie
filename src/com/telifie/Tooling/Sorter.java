package com.telifie.Tooling;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Sorter {
    public static void main(String[] args){
        Scanner in = new Scanner(System.in);
        String loc = in.nextLine();
        try {
            JSONTokener tokener = new JSONTokener(new FileReader(loc));
            JSONArray jsonArray = new JSONArray(tokener);
            List<JSONObject> jsonList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonList.add(jsonArray.getJSONObject(i));
            }
            jsonList.sort(Comparator.comparing((JSONObject obj) -> obj.getString("title"))
                    .thenComparingInt(obj -> obj.getInt("priority")));
            jsonArray = new JSONArray(jsonList);
            System.out.println(jsonArray.toString(4));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
