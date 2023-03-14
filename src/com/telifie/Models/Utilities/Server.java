package com.telifie.Models.Utilities;

import com.telifie.Models.Authentication;
import com.telifie.Models.Clients.AuthenticationClient;
import com.telifie.Models.Clients.ConnectorsClient;
import com.telifie.Models.Clients.UsersClient;
import com.telifie.Models.Connectors.Available.AWSS3;
import com.telifie.Models.Connectors.Connector;
import com.telifie.Models.Domain;
import com.telifie.Models.Result;
import org.apache.http.*;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;
import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private boolean running = true;
    private ServerSocket serverSocket;

    public Server(boolean threaded){

        if(threaded){

            Thread thread = new Thread(this::server);
            thread.start();
        }else{

            server();
        }
    }

    private void server() {

        HttpRequestHandler requestHandler = (request, response, context) -> {

            //Set Request variables
            String method = request.getRequestLine().getMethod();
            String query = request.getRequestLine().getUri().substring(1);
            String authString = (request.getFirstHeader("Authorization") == null ? "" : request.getFirstHeader("Authorization").getValue());
            Authentication auth = (authString.equals("") ? null : new Authentication(authString.split(" ")[1].split("\\.")));
            Result result = new Result(200, query, "\"okay\"");

            //Check if authentication is present
            if (auth == null) {

                result.setStatusCode(406);
                result.setResults("\"No Authentication credentials provided\"");
            } else {

                AuthenticationClient authenticationClient = new AuthenticationClient(new Domain("telifie", "mongodb://137.184.70.9:27017"));
                if (authenticationClient.isAuthenticated(auth)) {

                    String contentType = (request.getFirstHeader("Content-Type") == null ? "" : request.getFirstHeader("Content-Type").getValue());
                    if (contentType.equals("application/octet-stream")
                            || contentType.startsWith("multipart/form-data")) { //File upload

                        String director = request.getRequestLine().getUri(); //Where the file should go
                        ConnectorsClient connectors = new ConnectorsClient();
                        Connector awss3 = connectors.getConnector("AWS");

                        if (awss3 != null) {

                            if (request instanceof HttpEntityEnclosingRequest) {

                                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                                InputStream inputStream = entity.getContent();
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                                try {
                                    byte[] buffer = new byte[4096];
                                    int bytesRead;
                                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                                        outputStream.write(buffer, 0, bytesRead);
                                    }
                                } finally {
                                    inputStream.close();
                                    outputStream.close();
                                    EntityUtils.consume(entity);
                                }


                                byte[] fileContents = outputStream.toByteArray();
                                AWSS3 aws = new AWSS3(awss3);
                                if (aws.upload(director, fileContents, false)) {

                                    result = new Result(200, director, "\"" + awss3.getEndpoints().get(0).getUrl() + director + "\"");
                                } else {

                                    result = new Result(505, director, "\"Failed to upload file to provided director (AWS S3)\"");
                                }
                            }

                        } else {

                            result = new Result(505, director, "\"Please set AWS S3 Connector\"");
                        }

                    } else {

                        //Check if request has a body
                        String body = null;
                        if (request instanceof HttpEntityEnclosingRequest) {

                            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                            body = EntityUtils.toString(entity);
                        }

                        Configuration requestConfiguration = new Configuration();
                        requestConfiguration.addDomain(new Domain("telifie", "mongodb://137.184.70.9:27017"));
                        requestConfiguration.setAuthentication(auth);
                        UsersClient users = new UsersClient(requestConfiguration.defaultDomain()); //Ini UsersClient to find requesting user
                        requestConfiguration.setUser(users.getUserWithId(auth.getUser())); //Set Configuration User as requesting user
                        result = processRequest(requestConfiguration, method, query, body);
                    }
                } else {

                    result = new Result(403, "\"Invalid Auth Credentials\"");
                }
            }

            response.setStatusCode(result.getStatusCode());
            response.setHeader("Content-Type", result.getType());
            response.setEntity(new StringEntity(result.toString()));
        };

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, Integer.MAX_VALUE);
        HttpConnectionParams.setSoTimeout(params, Integer.MAX_VALUE);

        // Create a request handler registry
        HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
        registry.register("*", requestHandler);
        HttpService httpService = new HttpService(new BasicHttpProcessor(), new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory(), registry, params);
        try {

            serverSocket = new ServerSocket(80);
            while (running) {

                Socket socket = serverSocket.accept();
                DefaultBHttpServerConnection connection = new DefaultBHttpServerConnection(8 * 1024);
                connection.bind(socket);
                httpService.handleRequest(connection, new BasicHttpContext());
            }
        } catch (IOException | HttpException e) {
            throw new RuntimeException(e);
        }
    }

    private void stop(){

        running = false;
        try {

            serverSocket.close();
        } catch (IOException e) {

            throw new RuntimeException(e);
        }
    }

    private Result processRequest(Configuration configuration, String method, String request, String requestBody){

        //TODO log all requests from http to file
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