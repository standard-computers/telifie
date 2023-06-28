package com.telifie.Models.Utilities;

import com.telifie.Models.Actions.Command;
import com.telifie.Models.Clients.AuthenticationClient;
import com.telifie.Models.Clients.UsersClient;
import com.telifie.Models.Result;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Http {

    private ServerSocket serverSocket;
    private Socket socket;
    private Configuration config;

    public Http (Configuration config) {

        this.config = config;
        try {
            serverSocket = new ServerSocket(80);
            while (true) {
                socket = serverSocket.accept();
                Connection connection = new Connection();
                Thread th = new Thread(connection);
                th.start();
            }
        } catch (IOException e) {
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
        return new Result(404, request, "Invalid method");
    }
    private class Connection implements Runnable {

        @Override
        public void run(){
            HttpRequestHandler requestHandler = (request, response, context) -> {
                String method = request.getRequestLine().getMethod();
                String query = request.getRequestLine().getUri().substring(1);
                String authString = (request.getFirstHeader("Authorization") == null ? "" : request.getFirstHeader("Authorization").getValue());
                Authentication auth = (authString.equals("") ? null : new Authentication(authString.split(" ")[1].split("\\.")));
                Result result = new Result(200, query, "\"okay\"");

                if (auth == null) {
                    result.setStatusCode(406);
                    result.setResult("result", "No Authentication credentials provided");
                } else {
                    Configuration requestConfiguration = new Configuration();
                    requestConfiguration.setDomain(config.getDomain());
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
                        result = new Result(403, "Invalid Credentials");
                    }
                }
                response.setStatusCode(result.getStatusCode());
                response.setHeader("Content-Type", "application/json");
                response.setEntity(new StringEntity(result.toString()));
            };
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, Integer.MAX_VALUE);
            HttpConnectionParams.setSoTimeout(params, Integer.MAX_VALUE);
            HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
            registry.register("*", requestHandler);
            HttpService httpService = new HttpService(new BasicHttpProcessor(), new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory(), registry, params);
            try{
                DefaultBHttpServerConnection connection = new DefaultBHttpServerConnection(8 * 1024);
                connection.bind(socket);
                httpService.handleRequest(connection, new BasicHttpContext());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (HttpException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
