package com.matsuri.pricecache.demo;

import com.matsuri.pricecache.controller.PriceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Demo client application that simulates price publishers sending data to the cache service.
 * This demonstrates the API usage and can be used for testing the system.
 */
public class PriceCacheDemoClient {
    
    private static final String BASE_URL = "http://localhost:8080/api/prices";
    private static final String[] INSTRUMENTS = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"};
    private static final String[] VENDORS = {"BLOOMBERG", "REUTERS", "MARKIT", "ICE", "CME"};
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP"};
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random;
    private final ScheduledExecutorService scheduler;

    public PriceCacheDemoClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.random = new Random();
        this.scheduler = Executors.newScheduledThreadPool(5);
    }

    public void startDemo() {
        System.out.println("Starting Matsuri Price Cache Demo Client");
        System.out.println("========================================");
        
        // Publish some initial prices
        publishInitialPrices();
        
        // Start continuous price updates
        startContinuousPriceUpdates();
        
        // Demonstrate API queries
        demonstrateQueries();

        shutdown();
    }

    private void publishInitialPrices() {
        System.out.println("Publishing initial price data...");
        
        for (String instrument : INSTRUMENTS) {
            for (String vendor : VENDORS) {
                try {
                    PriceRequest request = generateRandomPrice(instrument, vendor);
                    publishPrice(request);
                    System.out.println("Published: " + instrument + " from " + vendor);
                    Thread.sleep(100); // Small delay to avoid overwhelming the service
                } catch (Exception e) {
                    System.err.println("Error publishing price for " + instrument + "/" + vendor + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("Initial prices published successfully");
    }

    private void startContinuousPriceUpdates() {
        System.out.println("Starting continuous price updates...");

        scheduler.scheduleAtFixedRate(() -> {
            try {
                String instrument = INSTRUMENTS[random.nextInt(INSTRUMENTS.length)];
                String vendor = VENDORS[random.nextInt(VENDORS.length)];
                PriceRequest request = generateRandomPrice(instrument, vendor);
                publishPrice(request);
                System.out.println("Updated: " + instrument + " from " + vendor + 
                                 " - Bid: " + request.getBidPrice() + ", Ask: " + request.getAskPrice());
            } catch (Exception e) {
                System.err.println("Error in continuous update: " + e.getMessage());
            }
        }, 5, 2, TimeUnit.SECONDS);
    }

    private void demonstrateQueries() {
        // Wait a bit for some data to be published
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        
        System.out.println("\nDemonstrating API queries...");
        System.out.println("============================");
        
        try {
            // Query prices by vendor
            System.out.println("Prices from BLOOMBERG:");
            String vendorPrices = restTemplate.getForObject(BASE_URL + "/vendor/BLOOMBERG", String.class);
            System.out.println(formatJson(vendorPrices));
            
            // Query prices by instrument
            System.out.println("\nPrices for AAPL:");
            String instrumentPrices = restTemplate.getForObject(BASE_URL + "/instrument/AAPL", String.class);
            System.out.println(formatJson(instrumentPrices));
            
            // Query specific price
            System.out.println("\nSpecific price (AAPL from BLOOMBERG):");
            String specificPrice = restTemplate.getForObject(BASE_URL + "/AAPL/BLOOMBERG", String.class);
            System.out.println(formatJson(specificPrice));
            
            // Get total count
            System.out.println("\nTotal prices in cache:");
            Integer count = restTemplate.getForObject(BASE_URL + "/count", Integer.class);
            System.out.println("Count: " + count);
            
        } catch (Exception e) {
            System.err.println("Error during query demonstration: " + e.getMessage());
        }
    }

    private PriceRequest generateRandomPrice(String instrument, String vendor) {
        // Generate realistic price data
        double basePrice = 50 + random.nextDouble() * 1000; // Price between 50 and 1050
        BigDecimal bidPrice = BigDecimal.valueOf(basePrice).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal askPrice = bidPrice.add(BigDecimal.valueOf(0.01 + random.nextDouble() * 0.5))
                                     .setScale(2, BigDecimal.ROUND_HALF_UP);
        
        String currency = CURRENCIES[random.nextInt(CURRENCIES.length)];
        
        return new PriceRequest(instrument, vendor, bidPrice, askPrice, LocalDateTime.now(), currency);
    }

    private void publishPrice(PriceRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String json = objectMapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            
            restTemplate.postForObject(BASE_URL, entity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish price", e);
        }
    }

    private String formatJson(String json) {
        try {
            Object obj = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return json; // Return original if formatting fails
        }
    }

    private void shutdown() {
        System.out.println("\nShutting down demo client...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        System.out.println("Demo client shutdown complete");
    }

    public static void main(String[] args) {
        new PriceCacheDemoClient().startDemo();
    }
}