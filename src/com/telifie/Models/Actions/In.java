package com.telifie.Models.Actions;

import java.io.*;
import java.util.Scanner;

public class In {

    public static String string(){
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    public static String string(String prompt){
        System.out.print(prompt);
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    public static Object serialized(String dir){
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(dir));
            Object obj = in.readObject();
            return obj;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
