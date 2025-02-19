package com.betting.stake;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.betting.session.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StakeHandler implements HttpHandler{
    
    private final SessionManager sessionManager;
    private final StakeStore stakeStore;
    
    public StakeHandler(SessionManager sessionManager, StakeStore stakeStore) {
        this.sessionManager = Objects.requireNonNull(sessionManager);
        this.stakeStore = Objects.requireNonNull(stakeStore);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, 0);// 405 Method Not Allowed
            exchange.close();
            return;
        }

        try {
            // Get session key from query params
            // Validate session key parameter
            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.contains("sessionkey=")) {
                exchange.sendResponseHeaders(400, 0);//Bad Request
                return;
            }
            
            String sessionKey = query.split("sessionkey=")[1];
            if (sessionKey.isEmpty() || !sessionManager.isValidSession(sessionKey)) {
                exchange.sendResponseHeaders(401, 0);//Unauthorized
                return;
            }

            // Parse bet offer ID from path
            // Parse and validate bet offer ID
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 2) {
                exchange.sendResponseHeaders(400, 0);
                return;
            }
            
            int betOfferId;
            try {
                betOfferId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                exchange.sendResponseHeaders(400, 0);
                return;
            }
            
            // Read stake value from request body
            // Read and validate stake value
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes());
            if (body.isEmpty()) {
                exchange.sendResponseHeaders(400, 0);
                return;
            }
            
            int stake;
            try {
                stake = Integer.parseInt(body);
            } catch (NumberFormatException e) {
                exchange.sendResponseHeaders(400, 0);
                return;
            }
            
            // Store the stake
            stakeStore.recordStake(betOfferId, sessionKey, stake);
            
            exchange.sendResponseHeaders(200, 0);
        } catch (Exception e) {
            exchange.sendResponseHeaders(400, 0);
        } finally {
            exchange.close();
        }
    }
}
