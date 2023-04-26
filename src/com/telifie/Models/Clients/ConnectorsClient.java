package com.telifie.Models.Clients;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.telifie.Models.Connectors.Connector;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.io.*;
import java.util.ArrayList;

public class ConnectorsClient {

    private final String workingDirectory = Telifie.getConnectorsDirectory();
    private ArrayList<Connector> connectors = new ArrayList<>();

    public ConnectorsClient(){

        File connectorsFolder;
        if(!(connectorsFolder = new File(workingDirectory)).exists()){

            connectorsFolder.mkdirs();
        }

        this.connectors.removeAll(connectors);
        File connectorsContainer = new File(this.workingDirectory);
        File[] connectorFiles = connectorsContainer.listFiles();
        for(File file : connectorFiles){

            try {

                Telifie.console.out.string("Importing Connector ->" + file.getPath());
                if(!file.getPath().contains(".DS_Store") && !file.isDirectory()) {

                    String json = new String(Files.readAllBytes(Paths.get(file.getPath())));
                    Document bsonDocument = Document.parse(json);
                    Connector connector = new Connector(bsonDocument);
                    this.connectors.add(connector);
                }
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    public boolean create(Connector connector){

        try (FileWriter file = new FileWriter(workingDirectory + connector.getId() + ".json")) {

            file.write(connector.toString());
            file.flush();
            file.close();
            return true;
        } catch (IOException e) {

            return false;
        }
    }

    public void save(Connector connector){

    }

    /**
     * Returns all available Connectors
     */
    public ArrayList<Connector> getConnectors(){

        return this.connectors;
    }

    public Connector getConnector(String name){

        if(connectors.size() > 0){

            for (Connector connector : connectors) {

                if(connector.getName().equals(name)){

                    return connector;
                }
            }
        }

        return null;
    }
}
