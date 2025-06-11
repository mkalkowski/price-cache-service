package com.matsuri.pricecache.service.impl;

import io.aeron.Publication;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;

public class AeronSendAgent implements Agent {
    private static final Logger logger = LoggerFactory.getLogger(AeronSendAgent.class);
    private final Publication publication;
    private final LinkedBlockingQueue<String> linkedBlockingQueue;

    public AeronSendAgent(final Publication publication, LinkedBlockingQueue<String> linkedBlockingQueue) {
        this.publication = publication;
        this.linkedBlockingQueue = linkedBlockingQueue;
    }

    @Override
    public int doWork() {

        try {
            String jsonMessage  = linkedBlockingQueue.take();
            byte[] messageBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
            UnsafeBuffer buffer = new UnsafeBuffer(messageBytes);
            buffer.putStringWithoutLengthUtf8(0, jsonMessage);
            if (publication.isConnected()) {
                long result = publication.offer(buffer);
                if (result <= 0) {
                    logger.warn("Failed to distribute price, result: {}", result); // assumption, do not reattempt
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return 0;
    }

    @Override
    public String roleName() {
        return "sender";
    }
}