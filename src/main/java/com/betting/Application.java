package com.betting;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.betting.session.SessionHandler;
import com.betting.session.SessionManager;
import com.betting.stake.HighStakesHandler;
import com.betting.stake.StakeHandler;
import com.betting.stake.StakeStoreManager;
import com.sun.net.httpserver.HttpServer;

public class Application {

    public static final int SERVER_PORT = 8001;

    public static int SESSION_TIMEOUT_MINUTES = 10;//default value 

    public static final AtomicBoolean systemOverloaded = new AtomicBoolean(false);

    public static void main(String[] args) throws IOException {

        //init the system monitor for the purpose of Rate Limiting
        ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();
        monitorExecutor.scheduleAtFixedRate(new SystemMonitor(), 0, 5, TimeUnit.SECONDS);
        
        parseArguments(args);

        // vritual thread pool for performance
        ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();

        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);

        // hand context
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

            if (systemOverloaded.get()) {// system overloaded. limite access
                String response = "Please try later!";
                exchange.sendResponseHeaders(503, response.getBytes().length);//503 busy
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                return;
            }
            
            String pathString = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (pathString.matches("/\\d+/session") && method.equals("GET")) {
                System.out.println("handler for get session!");
                 new SessionHandler().handle(exchange);

            } else if (pathString.matches("/\\d+/stake") && method.equals("POST")) {
                System.out.println("handler for create stake!");
                new StakeHandler(SessionManager.getInstance(), StakeStoreManager.getInstance()).handle(exchange);

            } else if (pathString.matches("/\\d+/highstakes") && method.equals("GET")) {
                System.out.println("handler for get hight stake!");
                new HighStakesHandler(StakeStoreManager.getInstance()).handle(exchange);

            } else {
                System.out.println("Not found!");
                exchange.sendResponseHeaders(404, 0); // not found
            }
            exchange.close();
        });
    }

    /**
     * shutdown server and thread pool gracefully
     * 
     * @param server
     * @param threadPool
     */
    private static void shutdownServerAndThreadPool(HttpServer server, ExecutorService threadPool) {
        System.out.println("Shutting down server...");
        server.stop(10);
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        System.out.println("Server and thread pool shut down.");
    }

    private static void parseArguments(String[] args) {
        String sessionTimeoutStr = System.getProperty("SESSION_TIMEOUT_MINUTES");

        if (sessionTimeoutStr != null) {
            try {
                SESSION_TIMEOUT_MINUTES = Integer.parseInt(sessionTimeoutStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid argument for SESSION_TIMEOUT_MINUTES: " + sessionTimeoutStr);
                System.exit(1);
            }
        }

    }
}