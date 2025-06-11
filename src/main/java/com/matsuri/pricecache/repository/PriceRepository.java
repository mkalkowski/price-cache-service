
package com.matsuri.pricecache.repository;

import com.matsuri.pricecache.domain.Price;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface PriceRepository {
    void save(Price price);
    Optional<Price> findByInstrumentAndVendor(String instrumentId, String vendorId);
    List<Price> findByVendor(String vendorId);
    List<Price> findByInstrument(String instrumentId);
    List<Price> findAll();
    void deleteOlderThan(LocalDateTime cutoffDate);
    int count();
    void clear();
}