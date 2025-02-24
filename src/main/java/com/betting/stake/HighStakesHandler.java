package com.betting.stake;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HighStakesHandler implements HttpHandler {

    private final StakeManager stakeStore;

    public HighStakesHandler(StakeManager stakeStore) {
        this.stakeStore = Objects.requireNonNull(stakeStore);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, 0);// 405 Method Not Allowed
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 3) {
            exchange.sendResponseHeaders(400, 0);// 400 Bad Request
            exchange.close();
            return;
        }

        try {
            int betofferid = Integer.parseInt(parts[1]);

            String hightStacks = stakeStore.getHighStakes(betofferid);

            exchange.sendResponseHeaders(200, hightStacks.length());

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(hightStacks.getBytes());
            }
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, 0);
        } finally {
            exchange.close();
        }
    }

}
