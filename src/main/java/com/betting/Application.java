package com.betting;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public class Application {

    public static final int SERVER_PORT = 8001;


    public static void main(String[] args) throws IOException {

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);

        server.createContext("/", exchange -> {
            String pathString = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if(pathString.matches("/\\d+/session") && method.equals("GET")){
                System.out.println("handler for get session!");
                exchange.sendResponseHeaders(200, 0);

            }else if (pathString.matches("/\\d+/stake") && method.equals("POST")) {
                System.out.println("handler for create stake!");
                exchange.sendResponseHeaders(200, 0);

            }else if (pathString.matches("/\\d+/hightstakes") && method.equals("GET")) {
                System.out.println("handler for get hight stake!");
                exchange.sendResponseHeaders(200, 0);

            } else{

                System.out.println("Not found!");
                exchange.sendResponseHeaders(404, 0);//not found
            }
            exchange.close();

        });

        server.setExecutor(threadPool);
        server.start();
        System.out.println("Server started at port " + SERVER_PORT);

    }
}