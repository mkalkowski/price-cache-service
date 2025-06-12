
package com.matsuri.pricecache.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.matsuri.pricecache.domain.Price;
import com.matsuri.pricecache.service.PriceDistributionService;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Aeron-based implementation for distributing price updates.
 * Uses Aeron's high-performance messaging for real-time price distribution.
 * Not optimal serialization and data model but just to proof of concept.
 */
@Service
public class AeronPriceDistributionService implements PriceDistributionService {

    private static final Logger logger = LoggerFactory.getLogger(AeronPriceDistributionService.class);

    private final ObjectMapper objectMapper;
    private LinkedBlockingQueue<String> linkedBlockingQueue;
    private Aeron aeron;
    private Publication publication;
    private MediaDriver mediaDriver;
    private AgentRunner sendAgentRunner;

    @Value("${aeron.channel:aeron:udp?endpoint=localhost:40123}")
    private String channel;

    @Value("${aeron.stream.id:1001}")
    private int streamId;

    @Value("${aeron.queueCapacity:1048576}")
    private int queueCapacity; // warning this may get full and block main thread

    public AeronPriceDistributionService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

    }

    @PostConstruct
    @Override
    public void start() {
        try {
            logger.info("Starting Aeron price distribution service");
            linkedBlockingQueue = new LinkedBlockingQueue<>(queueCapacity); // so much more to be improved than queue size - expiration policy, how to behave if full, competing consumers

            final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
                    .dirDeleteOnStart(true)
                    .threadingMode(ThreadingMode.SHARED)
                    .sharedIdleStrategy(new BusySpinIdleStrategy())
                    .dirDeleteOnShutdown(true);
            mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

            final Aeron.Context aeronCtx = new Aeron.Context()
                    .aeronDirectoryName(mediaDriver.aeronDirectoryName());

            logger.info("Aeron Dir {}", mediaDriver.aeronDirectoryName());
            aeron = Aeron.connect(aeronCtx);
            publication = aeron.addPublication(channel, streamId);
            final AeronSendAgent sendAgent = new AeronSendAgent(publication, linkedBlockingQueue);
            sendAgentRunner = new AgentRunner(new BusySpinIdleStrategy(),
                    Throwable::printStackTrace, null, sendAgent);
            AgentRunner.startOnThread(sendAgentRunner);
            logger.info("Aeron distribution service started successfully on channel: {}, streamId: {}",
                    channel, streamId);
        } catch (Exception e) {
            logger.error("Failed to start Aeron distribution service", e);
            throw new RuntimeException("Failed to initialize Aeron", e);
        }
    }

    @PreDestroy
    @Override
    public void stop() {
        logger.info("Stopping Aeron price distribution service");
        if (sendAgentRunner != null) {
            sendAgentRunner.close();
        }

        if (publication != null) {
            publication.close();
        }

        if (aeron != null) {
            aeron.close();
        }

        if (mediaDriver != null) {
            mediaDriver.close();
        }

        logger.info("Aeron distribution service stopped");
    }

    @Override
    public void distributePrice(Price price) {
        if (publication == null || !publication.isConnected()) {
            logger.error("Publication not available, skipping price distribution");
            return;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(price);
            linkedBlockingQueue.offer(jsonMessage);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize price for distribution", e);
        }
    }
}