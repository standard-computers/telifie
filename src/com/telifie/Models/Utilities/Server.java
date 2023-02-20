package com.telifie.Models.Utilities;

import com.telifie.Models.Authentication;
import com.telifie.Models.Clients.AuthenticationClient;
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

    public Server(boolean threaded){

        if(threaded){

            Thread thread = new Thread(this::server);
            thread.start();

        }else{

            server();

        }

    }

    private void server(){

        HttpRequestHandler requestHandler = (request, response, context) -> {

            String method = request.getRequestLine().getMethod();
            String query = request.getRequestLine().getUri().substring(1);

            String authString = (request.getFirstHeader("Authorization") == null ? "" : request.getFirstHeader("Authorization").getValue());
            Authentication auth = (authString.equals("") ? null : new Authentication(authString.split(" ")[1].split("\\.")));

            Result result = new Result(200, query,"\"okay\"");

            if(auth == null){

                result.setStatusCode(406);
                result.setResults("\"No Authentication credentials provided\"");

            }else{

                //Check if request has a body
                String body = null;
                if (request instanceof HttpEntityEnclosingRequest) {

                    HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                    body = EntityUtils.toString(entity);

                }

                AuthenticationClient authenticationClient = new AuthenticationClient(new Domain("telifie", "mongodb://137.184.70.9:27017"));
                if(authenticationClient.isAuthenticated(auth)){

                    Configuration requestConfiguration = new Configuration();
                    //TODO reconfigure configuration of request
                    requestConfiguration.addDomain(new Domain("telifie", "mongodb://137.184.70.9:27017"));
                    requestConfiguration.setAuthentication(auth);
                    result = processRequest(requestConfiguration, method, query, body);

                }else{

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
        HttpService httpService = new HttpService(
                new BasicHttpProcessor(),
                new DefaultConnectionReuseStrategy(),
                new DefaultHttpResponseFactory(),
                registry, params);
        ServerSocket serverSocket;
        try {

            serverSocket = new ServerSocket(80);
            while (true) {

                Socket socket = serverSocket.accept();
                DefaultBHttpServerConnection connection = new DefaultBHttpServerConnection(8 * 1024);
                connection.bind(socket);
                httpService.handleRequest(connection, new BasicHttpContext());

            }

        } catch (IOException | HttpException e) {

            //TODO log and don't quit server
            throw new RuntimeException(e);

        }

    }

    private Result processRequest(Configuration configuration, String method, String request, String requestBody){

        //TODO log all requests from http to file
        Command command = new Command(request);
        if(command.primarySelector().equals("exit") || command.primarySelector().equals("server")){

            return new Result(403, request, "\"Illegal command given\"");

        }else if(method.equals("POST")){

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