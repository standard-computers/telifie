package com.telifie.Models.Utilities;

import com.telifie.Models.Actions.Command;
import com.telifie.Models.Clients.AuthenticationClient;
import com.telifie.Models.Clients.UsersClient;
import com.telifie.Models.Result;
import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import javax.net.ssl.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;

public class Server {

    private static final int PORT = 443;
    private static final String KEYSTORE_PATH = "/opt/telifie/ssl/telifie.jks";
    private static final String KEYSTORE_PASSWORD = "JxBwCQZTHx5suZ8W";
    private SSLSocket clientSocket;
    private Configuration config;

    public Server(Configuration config){
        this.config = config;
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance("JKS");
            InputStream inputStream = new FileInputStream(KEYSTORE_PATH);
            keyStore.load(inputStream, KEYSTORE_PASSWORD.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD.toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
            SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(PORT);
            serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
            serverSocket.setNeedClientAuth(false);
            while (true) {
                try {
                    clientSocket = (SSLSocket) serverSocket.accept();
                    Connection connection = new Connection();
                    Thread th = new Thread(connection);
                    th.start();
                } catch (IOException e) {
                    System.out.println("Failed to accept client connection: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        } catch (KeyStoreException e) {
            System.err.println("Failed to ini SSL Server: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException | KeyManagementException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Result processRequest(Configuration configuration, String method, String request, String requestBody){
        if(method.equals("POST")){
            try {
                return new Command(request).parseCommand(configuration, Document.parse(requestBody));
            }catch(BsonInvalidOperationException e){
                return new Result(505, "Malformed JSON data provided");
            }
        }else if(method.equals("GET")){
            return new Command(request).parseCommand(configuration, null);
        }
        return new Result(404, request, "Invalid HTTP request method");
    }

    private class Connection implements Runnable {

        @Override
        public void run() {
            try {

                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String requestLine = input.readLine();
                String headers = "", line;
                while (!(line = input.readLine()).equals("")) {
                    headers += line + "\r\n";
                }
                String authString = "";
                String[] headerLines = headers.split("\r\n");
                for (String headerLine : headerLines) {
                    if (headerLine.startsWith("Authorization:")) {
                        authString = headerLine.substring("Authorization:".length()).trim();
                    }
                }
                Authentication auth = (authString.equals("") ? null : new Authentication(authString.split(" ")[1].split("\\.")));
                String method = requestLine.split(" ")[0];
                String query = URLDecoder.decode(requestLine.split(" ")[1].substring(1), StandardCharsets.UTF_8);
                Result result = new Result(200, query, "\"okay\"");
                if (authString.equals("") || auth == null) {
                    result.setStatusCode(406);
                    result.setResult("result", "No Authentication credentials provided");
                } else {
                    StringBuilder requestBody = new StringBuilder();
                    if (headers.contains("Content-Length")) {
                        int contentLength = Integer.parseInt(headers.split("Content-Length: ")[1].split("\r\n")[0]);
                        char[] buffer = new char[contentLength];
                        input.read(buffer);
                        requestBody.append(buffer);
                    }
                    Configuration requestConfiguration = new Configuration();
                    requestConfiguration.setDomain(config.getDomain());
                    AuthenticationClient authenticationClient = new AuthenticationClient(requestConfiguration);
                    if (authenticationClient.isAuthenticated(auth)) {
                        requestConfiguration.setAuthentication(auth);
                        UsersClient users = new UsersClient(requestConfiguration); //Ini UsersClient to find requesting user
                        requestConfiguration.setUser(users.getUserWithId(auth.getUser())); //Set Configuration User as requesting user
                        result = processRequest(requestConfiguration, method, query, requestBody.toString());
                    } else {
                        result = new Result(403, "Invalid Auth Credentials");
                    }
                }
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json; charset=utf-8");
                out.println("Access-Control-Allow-Origin: *"); // Allow all origins
                out.println("Access-Control-Allow-Headers: Authorization");
                out.println("Connection: close");
                out.println();
                out.println(result.toString());
                out.flush();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Failed to process client request: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}