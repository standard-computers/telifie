package com.telifie.Models.Utilities;


import com.telifie.Models.Actions.Command;
import com.telifie.Models.Clients.AuthenticationClient;
import com.telifie.Models.Clients.UsersClient;
import com.telifie.Models.Domain;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.Authentication;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Telifie;
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

    public Server() {
        Telifie.console.out.string("Starting SSL server...");
        // Load keystore
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("JKS");
            InputStream inputStream = new FileInputStream(KEYSTORE_PATH);
            keyStore.load(inputStream, KEYSTORE_PASSWORD.toCharArray());

            // Initialize key manager factory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

            // Initialize SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            // Initialize server socket factory
            SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(PORT);

            // Set SSL parameters
            serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
            serverSocket.setNeedClientAuth(false);

            // Listen for client connections
            Telifie.console.out.string("Attempting SSL server connection on port " + PORT + "...");
            while (true) {
                Telifie.console.out.string("Waiting for client connection...");
                try {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    Telifie.console.out.string("Accepting new SSL Client -> " + clientSocket.getInetAddress().getHostAddress());

                    //Handle client request
                    BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String requestLine = input.readLine();

                    //Read request headers
                    String headers = "";
                    String line;
                    while (!(line = input.readLine()).equals("")) {
                        headers += line + "\r\n";
                    }

                    //Extract authorization header from request headers
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
                        requestConfiguration.setDomain(new Domain("telifie", "mongodb://137.184.70.9:27017"));
                        AuthenticationClient authenticationClient = new AuthenticationClient(requestConfiguration);
                        if (authenticationClient.isAuthenticated(auth)) {

                            requestConfiguration.setAuthentication(auth);
                            UsersClient users = new UsersClient(requestConfiguration); //Ini UsersClient to find requesting user
                            requestConfiguration.setUser(users.getUserWithId(auth.getUser())); //Set Configuration User as requesting user
                            result = processRequest(requestConfiguration, method, query, requestBody.toString());
                        } else {

                            result = new Result(403, "\"Invalid Auth Credentials\"");
                        }
                    }
                    // Set the content type of the response to "application/json"
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
                    Telifie.console.out.string("Failed to accept client connection: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        } catch (KeyStoreException e) {
            Telifie.console.out.error("Failed to ini SSL Server: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException | KeyManagementException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Result processRequest(Configuration configuration, String method, String request, String requestBody){
        Command command = new Command(request);
        if(method.equals("POST")){
            try {
                return command.parseCommand(configuration, Document.parse(requestBody));
            }catch(BsonInvalidOperationException e){
                return new Result(505, "\"Malformed JSON data provided\"");
            }
        }else if(method.equals("GET")){
            return command.parseCommand(configuration);
        }
        return new Result(404, request, "\"Invalid command received\"");
    }
}