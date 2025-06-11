
package com.matsuri.pricecache.service;

import com.matsuri.pricecache.domain.Price;

/**
 * Service interface for distributing price updates to downstream systems.
 */
public interface PriceDistributionService {
    void distributePrice(Price price);
    void start();
    void stop();
}