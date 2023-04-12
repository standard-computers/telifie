package com.telifie.Models.Actions;

import com.telifie.Models.Clients.AuthenticationClient;
import com.telifie.Models.Clients.UsersClient;
import com.telifie.Models.Domain;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.Authentication;
import com.telifie.Models.Utilities.Configuration;
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

public class HttpServer {

    private boolean running = true;
    private ServerSocket serverSocket;

    public HttpServer() {

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
                result.setResult("result", "No Authentication credentials provided");
            } else {

                Configuration requestConfiguration = new Configuration();
                requestConfiguration.setDomain(new Domain("telifie", "mongodb://137.184.70.9:27017"));

                //TODO make sure to import from file.
                //new Domain("telifie", "mongodb://137.184.70.9:27017")
                AuthenticationClient authenticationClient = new AuthenticationClient(requestConfiguration);
                if (authenticationClient.isAuthenticated(auth)) {

                    String body = null;
                    if (request instanceof HttpEntityEnclosingRequest) {

                        HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                        body = EntityUtils.toString(entity);
                    }
                    requestConfiguration.setAuthentication(auth);
                    UsersClient users = new UsersClient(requestConfiguration); //Ini UsersClient to find requesting user
                    requestConfiguration.setUser(users.getUserWithId(auth.getUser())); //Set Configuration User as requesting user
                    result = processRequest(requestConfiguration, method, query, body);
                } else {

                    result = new Result(403, "\"Invalid Auth Credentials\"");
                }
            }

            response.setStatusCode(result.getStatusCode());
            response.setHeader("Content-Type", "application/json");
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
        } catch(ConnectionClosedException e){

            try {

                serverSocket.close();
                new HttpServer();

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }catch (IOException | HttpException e) {
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