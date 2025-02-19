package com.betting;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

public class Application {

    public static final int SERVER_PORT = 8001;

    public static void main(String[] args) throws IOException {
        //vritual thread pool for performance
        ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();

        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);

        //hand context
        createContext(server);

        server.setExecutor(threadPool);
        server.start();
        System.out.println("Server started at port " + SERVER_PORT);

        // graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownServerAndThreadPool(server, threadPool);
        }));
    }

    private static void createContext(HttpServer server) {
        server.createContext("/", exchange -> {
            String pathString = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (pathString.matches("/\\d+/session") && method.equals("GET")) {
                System.out.println("handler for get session!");
                exchange.sendResponseHeaders(200, 0);

            } else if (pathString.matches("/\\d+/stake") && method.equals("POST")) {
                System.out.println("handler for create stake!");
                exchange.sendResponseHeaders(200, 0);

            } else if (pathString.matches("/\\d+/hightstakes") && method.equals("GET")) {
                System.out.println("handler for get hight stake!");
                exchange.sendResponseHeaders(200, 0);

            } else {
                System.out.println("Not found!");
                exchange.sendResponseHeaders(404, 0); // not found
            }
            exchange.close();
        });
    }

    

    /**
     * shutdown server and thread pool gracefully 
     * @param server
     * @param threadPool
     */
    private static void shutdownServerAndThreadPool(HttpServer server, ExecutorService threadPool) {
        System.out.println("Shutting down server...");
        server.stop(10); 
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow(); 
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        System.out.println("Server and thread pool shut down.");
    }
}