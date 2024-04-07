package com.telifie.Tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Clients.Packages;
import com.telifie.Models.Clients.UsersClient;
import com.telifie.Models.User;
import com.telifie.Models.Utilities.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class CreateUser {

    private static Configuration config;
    private static final File configFile = new File(Telifie.configDirectory() + "/config.json");

    public static void main(String[] args){
        checkConfig();
        new ini();
    }

    public static class ini extends JFrame {
        public ini(){
            setTitle("Create Telifie User");
            JLabel nameLabel = new JLabel("NAME");
            JTextField name = new JTextField("", 20);
            JLabel emailLabel = new JLabel("EMAIL");
            JTextField email = new JTextField("", 20);
            JButton confirmButton = new JButton("CREATE");
            setLayout(new FlowLayout(FlowLayout.CENTER));
            add(nameLabel);
            add(name);
            add(emailLabel);
            add(email);
            add(confirmButton);
            setSize(275,230);
            setLocationRelativeTo(null);
            setVisible(true);
            confirmButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    User u = new User(email.getText(), name.getText(), "");
                    u.setPermissions(3);
                    UsersClient users = new UsersClient();
                    users.create(u);
                    Log.console("Successfully Created User");
                    setVisible(false);
                    new ini();
                }
            });
        }
    }

    private static void checkConfig(){
        if(configFile.exists()){
            Console.message("Config file found :)");
            importConfiguration();
            if (config != null) {
                config.startMongo();
                config.startSql();
                new Packages(new Session("com.telifie.system", "telifie"));
                new Andromeda();
            }else{
                Log.error("FAILED CONFIG FILE LOAD", "CLIx110");
                System.exit(-1);
            }
        }else{
            Console.message("No config file found. Use option '--install'");
        }
    }

    private static void importConfiguration(){
        try {
            config = new ObjectMapper().readValue(configFile, Configuration.class);
        } catch (IOException e) {
            Log.error("FAILED CONFIG.JSON IMPORT", "CLIx106");
        }
    }
}
