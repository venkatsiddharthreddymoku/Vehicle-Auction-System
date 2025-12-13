package com.vehicleauction;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WebServer {
    private AuctionManager auctionManager;
    
    public WebServer(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }
    
    public void start(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // API endpoints
            server.createContext("/api/vehicles", new VehiclesHandler());
            server.createContext("/api/vehicles/active", new ActiveVehiclesHandler());
            server.createContext("/api/bid", new BidHandler());
            server.createContext("/api/register", new RegisterHandler());
            server.createContext("/api/login", new LoginHandler());
            server.createContext("/api/user", new UserHandler());
            
            // Static file serving
            server.createContext("/", new StaticFileHandler());
            
            server.setExecutor(null);
            server.start();
            
            System.out.println("Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private class VehiclesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = auctionManager.getVehiclesJSON();
                sendResponse(exchange, 200, response);
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }
    
    private class ActiveVehiclesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = auctionManager.getActiveVehiclesJSON();
                sendResponse(exchange, 200, response);
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }
    
    private class BidHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String requestBody = readRequestBody(exchange);
                Map<String, String> params = parseFormData(requestBody);
                
                try {
                    int vehicleId = Integer.parseInt(params.get("vehicleId"));
                    int userId = Integer.parseInt(params.get("userId"));
                    double amount = Double.parseDouble(params.get("amount"));
                    
                    boolean success = auctionManager.placeBid(vehicleId, userId, amount);
                    
                    if (success) {
                        sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Bid placed successfully\"}");
                    } else {
                        sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Bid must be higher than current bid or auction has ended\"}");
                    }
                } catch (NumberFormatException e) {
                    sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid parameters\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }
    
    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String requestBody = readRequestBody(exchange);
                Map<String, String> params = parseFormData(requestBody);
                
                String name = params.get("name");
                String email = params.get("email");
                String password = params.get("password");
                
                User user = auctionManager.registerUser(name, email, password);
                
                if (user != null) {
                    sendResponse(exchange, 200, user.toJSON());
                } else {
                    sendResponse(exchange, 400, "{\"error\":\"Email already exists\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }
    
    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String requestBody = readRequestBody(exchange);
                Map<String, String> params = parseFormData(requestBody);
                
                String email = params.get("email");
                String password = params.get("password");
                
                User user = auctionManager.authenticateUser(email, password);
                
                if (user != null) {
                    sendResponse(exchange, 200, user.toJSON());
                } else {
                    sendResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }
    
    private class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                
                try {
                    int userId = Integer.parseInt(params.get("id"));
                    String response = auctionManager.getUserJSON(userId);
                    sendResponse(exchange, 200, response);
                } catch (NumberFormatException e) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid user ID\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }
    
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            // Serve static files from web directory
            InputStream is = getClass().getResourceAsStream("/web" + path);
            
            if (is != null) {
                // Determine content type
                String contentType = "text/html";
                if (path.endsWith(".css")) {
                    contentType = "text/css";
                } else if (path.endsWith(".js")) {
                    contentType = "application/javascript";
                } else if (path.endsWith(".png")) {
                    contentType = "image/png";
                } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                }
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, 0);
                
                OutputStream os = exchange.getResponseBody();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.close();
                is.close();
            } else {
                // File not found
                String response = "File not found: " + path;
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
    
    // Utility methods
    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
    
    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                try {
                    String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                    String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name());
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    // UTF-8 should always be supported
                }
            }
        }
        return params;
    }
    
    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}