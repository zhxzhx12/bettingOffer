package com.betting;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.betting.session.SessionHandler;
import com.betting.session.SessionManager;
import com.betting.stake.HighStakesHandler;
import com.betting.stake.StakeHandler;
import com.betting.stake.StakeManager;
import com.betting.systemmanager.SystemMonitor;
import com.sun.net.httpserver.HttpServer;

public class Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);

    public static final int SERVER_PORT = 8001;

    public static int SESSION_TIMEOUT_MINUTES = 10;//default value 

    public static final AtomicBoolean systemOverloaded = new AtomicBoolean(false);

    public static void main(String[] args) throws IOException {

        //init the system monitor for the purpose of Rate Limiting
        ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
        monitorExecutor.scheduleAtFixedRate(new SystemMonitor(), 0, 2, TimeUnit.SECONDS);
        
        //parse arguments which can be set by for example: -DSESSION_TIMEOUT_MINUTES=10
        parseArguments(args);

        // vritual thread pool for performance
        ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();

        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);

        // hand context
        createContext(server);

        server.setExecutor(threadPool);
        server.start();

        logger.info("Server started at port {}", SERVER_PORT);

        // graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownServerAndThreadPool(server, threadPool);
        }));
    }

    private static void createContext(HttpServer server) {

        server.createContext("/", exchange -> {

            if (systemOverloaded.get()) {// system overloaded. limite access
                logger.warn("System overloaded, please try later!");
                String response = "Please try later!";
                exchange.sendResponseHeaders(503, response.getBytes().length);//503 busy
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                return;
            }
            
            try {
                
                String pathString = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
    
                if (pathString.matches("/\\d+/session") && method.equals("GET")) {
                    logger.info("handler for get session!");
                     new SessionHandler().handle(exchange);
    
                } else if (pathString.matches("/\\d+/stake") && method.equals("POST")) {
                    logger.info("handler for create stake!");
                    new StakeHandler(SessionManager.getInstance(), StakeManager.getInstance()).handle(exchange);
    
                } else if (pathString.matches("/\\d+/highstakes") && method.equals("GET")) {
                    logger.info("handler for get hight stake!");
                    new HighStakesHandler(StakeManager.getInstance()).handle(exchange);
    
                } else {
                    logger.error("{} {} not found!", method, pathString);
                    exchange.sendResponseHeaders(404, 0); // not found
                }
            } catch (Exception e) {
                logger.error("Error:", e);
            }finally{
                exchange.close();
            }   
        });
    }

    /**
     * shutdown server and thread pool gracefully
     * 
     * @param server
     * @param threadPool
     */
    private static void shutdownServerAndThreadPool(HttpServer server, ExecutorService threadPool) {
        logger.info("Shutting down server...");
        server.stop(10);
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        logger.info("Server and thread pool shut down.");
    }

    /**
     * Currently only support SESSION_TIMEOUT_MINUTES, can be extended to support more
     * @param args
     */
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