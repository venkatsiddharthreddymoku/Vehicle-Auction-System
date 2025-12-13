package com.vehicleauction;

public class Main {
    public static void main(String[] args) {
        System.out.println("🚗 Starting Vehicle Auction System...");
        
        try {
            AuctionManager auctionManager = new AuctionManager();
            WebServer webServer = new WebServer(auctionManager);
            webServer.start(8080);
            
            System.out.println("✅ Vehicle Auction System running on http://localhost:8080");
            System.out.println("📧 Sample users: john@example.com / jane@example.com (password: password123)");
        } catch (Exception e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}