
package com.matsuri.pricecache.service;

import com.matsuri.pricecache.domain.Price;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for price cache operations.
 * This defines the business operations available for price management.
 */
public interface PriceCacheService {
    void publishPrice(Price price);
    Optional<Price> getPrice(String instrumentId, String vendorId);
    List<Price> getPricesByVendor(String vendorId);
    List<Price> getPricesByInstrument(String instrumentId);
    List<Price> getAllPrices();
    void cleanupOldPrices();
    int getPriceCount();
}
