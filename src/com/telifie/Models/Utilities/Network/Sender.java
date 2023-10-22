package com.telifie.Models.Utilities.Network;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

public class Sender {

    private int port = 1234;

    public Sender(int port, String psswd){
        this.port = port;

        char[] keystorePassword = psswd.toCharArray();
        String keystorePath = "server_keystore.jks";
        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(keystorePath), keystorePassword);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keystorePassword);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);
            System.out.println("Server is listening on port " + port);
            while (true) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                System.out.println("Connected to " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}


class ClientHandler extends Thread {
    private final SSLSocket socket;

    public ClientHandler(SSLSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                out.println("Server: " + message);
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
