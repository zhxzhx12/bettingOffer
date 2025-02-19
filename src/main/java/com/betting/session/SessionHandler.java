package com.betting.session;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class SessionHandler implements HttpHandler{
    
    private final SessionManager sessionManager;
    
    public SessionHandler() {
        this.sessionManager = SessionManager.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 3) {
            exchange.sendResponseHeaders(400, 0);
            exchange.close();
            return;
        }

        try {
            int customerId = Integer.parseInt(parts[1]);
            String sessionKey = sessionManager.getOrCreateSession(customerId);
            
            exchange.sendResponseHeaders(200, sessionKey.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(sessionKey.getBytes());
            }
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, 0);
        } finally {
            exchange.close();
        }
    }
}
