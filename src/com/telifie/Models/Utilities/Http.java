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

    private Socket socket;

    public Http () {
        try {
            ServerSocket serverSocket = new ServerSocket(80);
            while (true) {
                socket = serverSocket.accept();
                Connection connection = new Connection();
                Thread th = new Thread(connection);
                th.start();
            }
        } catch (IOException e) {
            Log.error("FAILED STARTING HTTP SERVER ON :80");
        }
    }

    private Result processRequest(Session session, String method, String request, String requestBody){
        if(method.equals("POST")){
            try {
                return new Command(request).parseCommand(session, Document.parse(requestBody));
            }catch(BsonInvalidOperationException e){
                return new Result(505, "Malformed JSON data provided");
            }
        }else if(method.equals("GET")){
            return new Command(request).parseCommand(session, null);
        }
        return new Result(404, request, "Invalid method");
    }

    private class Connection implements Runnable {

        @Override
        public void run(){
            HttpRequestHandler requestHandler = (request, response, context) -> {
                String query = request.getRequestLine().getUri().substring(1);
                Authentication auth = (request.getFirstHeader("Authorization") == null ? null : new Authentication(request.getFirstHeader("Authorization").getValue()));
                Result result = new Result(200, query, "OK");
                Session session;
                if (auth == null) {
                    result.setStatusCode(406);
                    result.setResult("result", "NO AUTH PROVIDED");
                } else {
                    AuthenticationClient authenticationClient = new AuthenticationClient();
                    if (authenticationClient.isAuthenticated(auth)) {
                        String body = null;
                        if (request instanceof HttpEntityEnclosingRequest) {
                            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                            body = EntityUtils.toString(entity);
                        }
                        UsersClient users = new UsersClient();
                        session = new Session(users.getUserWithId(auth.getUser()).getId(), "telifie");
                        result = processRequest(session, request.getRequestLine().getMethod(), query, body);
                    } else {
                        result = new Result(403, "INVALID CREDENTIALS");
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
            } catch (IOException | HttpException e) {
                Log.error("CONNECTION INTERUPTED");
            }
        }
    }
}