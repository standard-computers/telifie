package com.telifie.Models.Utilities;

import com.telifie.Models.Actions.Out;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;

public class HttpsServer {

    public HttpsServer(Configuration configuration, boolean threaded){

        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket serverSocket = null;
        try {
            serverSocket = (SSLServerSocket) factory.createServerSocket(443);
            serverSocket.setEnabledCipherSuites(factory.getSupportedCipherSuites());

            while (true) {
                // Accept a new connection
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                socket.startHandshake();

                // Get the input and output streams for the socket
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Read the incoming HTTP request
                String request = in.readLine();
                Out.console(request);
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    Out.console(line);
                }

                // Send an HTTP response
                out.write("HTTP/1.1 200 OK\r\n");
                out.write("Content-Type: text/plain\r\n");
                out.write("\r\n");
                out.write("Hello, world!\r\n");
                out.flush();

                // Close the socket
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
