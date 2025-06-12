
package com.matsuri.pricecache.service.impl;

import com.matsuri.pricecache.service.PriceCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled cleanup of old prices.
 * Automatically removes prices older than 30 days on a daily basis.
 */
@Service
public class PriceCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(PriceCleanupService.class);
    
    private final PriceCacheService priceCacheService;

    @Autowired
    public PriceCleanupService(PriceCacheService priceCacheService) {
        this.priceCacheService = priceCacheService;
    }

    @Scheduled(cron = "${cleanup.schedule}")
    public void scheduledCleanup() {
        long start = System.currentTimeMillis();
        logger.info("Starting scheduled price cleanup");
        try {
            priceCacheService.cleanupOldPrices();
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            logger.info("Scheduled price cleanup completed successfully in {} ms", timeElapsed);
        } catch (Exception e) {
            logger.error("Error during scheduled price cleanup", e);
        }
    }
}