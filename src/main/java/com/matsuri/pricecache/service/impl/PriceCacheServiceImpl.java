
package com.matsuri.pricecache.service.impl;

import com.matsuri.pricecache.domain.Price;
import com.matsuri.pricecache.repository.PriceRepository;
import com.matsuri.pricecache.service.PriceCacheService;
import com.matsuri.pricecache.service.PriceDistributionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of PriceCacheService.
 * This service orchestrates price storage, retrieval, and distribution operations.
 */
@Service
public class PriceCacheServiceImpl implements PriceCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(PriceCacheServiceImpl.class);

    @Value("${cleanup.retentionDays:30}")
    private int retentionDays;

    private final PriceRepository priceRepository;
    private final PriceDistributionService distributionService;

    @Autowired
    public PriceCacheServiceImpl(PriceRepository priceRepository, 
                                PriceDistributionService distributionService) {
        this.priceRepository = priceRepository;
        this.distributionService = distributionService;
    }

    @Override
    public void publishPrice(Price price) {
        logger.debug("Publishing price: {}", price);

        priceRepository.save(price);
        
        // Distribute to interested parties - assumption distribute (PUSH) all
        distributionService.distributePrice(price);
        
        logger.info("Price published successfully for instrument {} from vendor {}", 
                   price.getInstrumentId(), price.getVendorId());
    }

    @Override
    public Optional<Price> getPrice(String instrumentId, String vendorId) {
        logger.debug("Retrieving price for instrument {} from vendor {}", instrumentId, vendorId);
        return priceRepository.findByInstrumentAndVendor(instrumentId, vendorId);
    }

    @Override
    public List<Price> getPricesByVendor(String vendorId) {
        logger.debug("Retrieving all prices from vendor {}", vendorId);
        return priceRepository.findByVendor(vendorId);
    }

    @Override
    public List<Price> getPricesByInstrument(String instrumentId) {
        logger.debug("Retrieving all prices for instrument {}", instrumentId);
        return priceRepository.findByInstrument(instrumentId);
    }

    @Override
    public List<Price> getAllPrices() {
        logger.debug("Retrieving all prices");
        return priceRepository.findAll();
    }

    @Override
    public void cleanupOldPrices() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        logger.info("Cleaning up prices older than {}", cutoffDate);
        
        int countBefore = priceRepository.count();
        priceRepository.deleteOlderThan(cutoffDate);
        int countAfter = priceRepository.count();
        
        logger.info("Cleanup complete. Removed {} prices", countBefore - countAfter);
    }

    @Override
    public int getPriceCount() {
        return priceRepository.count();
    }
}