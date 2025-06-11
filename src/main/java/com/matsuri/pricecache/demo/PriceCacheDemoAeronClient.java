package com.matsuri.pricecache.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.matsuri.pricecache.controller.PriceRequest;
import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Demo of Aeron connectivity. See README.md for running instruction.
 */
public class PriceCacheDemoAeronClient {

    private static final String BASE_URL = "http://localhost:8080/api/prices";
    private static final String AERON_URL = "aeron:udp?endpoint=localhost:40123";
    private static final int AERON_STREAM = 1001;

    private static final String[] INSTRUMENTS = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"};
    private static final String[] VENDORS = {"BLOOMBERG", "REUTERS", "MARKIT", "ICE", "CME"};
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP"};

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random;
    private AgentRunner receiveAgentRunner;
    private Aeron aeron;
    private MediaDriver mediaDriver;

    public PriceCacheDemoAeronClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.random = new Random();
    }

    public void startDemo() {
        System.out.println("Starting Matsuri Price Cache Demo Aeron Client");
        System.out.println("========================================");
        
        startAeronClient();

        publishInitialPrices();

        shutdown();
    }

    private void startAeronClient() {
        final IdleStrategy idleStrategyReceive = new BusySpinIdleStrategy();

        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
                .dirDeleteOnStart(true)
                .threadingMode(ThreadingMode.SHARED)
                .sharedIdleStrategy(new BusySpinIdleStrategy())
                .dirDeleteOnShutdown(true);
        mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        final Aeron.Context aeronCtx = new Aeron.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName());

        System.out.println("Aeron Dir " + mediaDriver.aeronDirectoryName());

        aeron = Aeron.connect(aeronCtx);

        final Subscription subscription = aeron.addSubscription(AERON_URL, AERON_STREAM);

        final AeronReceiveAgent aeronReceiveAgent = new AeronReceiveAgent(subscription);

        receiveAgentRunner = new AgentRunner(idleStrategyReceive,
                Throwable::printStackTrace, null, aeronReceiveAgent);
        System.out.println("starting");
        //start the runners
        AgentRunner.startOnThread(receiveAgentRunner);
    }

    private void publishInitialPrices() {
        System.out.println("Publishing initial price data...");
        int messageCount = 0;
        for (String instrument : INSTRUMENTS) {
            for (String vendor : VENDORS) {
                try {
                    PriceRequest request = generateRandomPrice(instrument, vendor);
                    publishPrice(request);
                    messageCount += 1;
                    System.out.println("Published: " + instrument + " from " + vendor);
                    Thread.sleep(100); // Small delay to avoid overwhelming the service
                } catch (Exception e) {
                    System.err.println("Error publishing price for " + instrument + "/" + vendor + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("===========================================");
        System.out.println("Prices published successfully. Count: " + messageCount);
    }

    private PriceRequest generateRandomPrice(String instrument, String vendor) {
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

    private void shutdown() {
        System.out.println("Shutting down demo client...");
        receiveAgentRunner.close();
        aeron.close();
        mediaDriver.close();
        System.out.println("Demo client shutdown complete");
    }

    public static void main(String[] args) {
        new PriceCacheDemoAeronClient().startDemo();
    }
}