
package com.matsuri.pricecache.repository.impl;

import com.matsuri.pricecache.domain.Price;
import com.matsuri.pricecache.repository.PriceRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of PriceRepository using ConcurrentHashMap.
 * This implementation is thread-safe and can be easily replaced with a database implementation.
 */
@Repository
public class InMemoryPriceRepository implements PriceRepository {
    
    // Using composite key (instrumentId_vendorId) for O(1) lookups
    private final Map<String, Price> priceStore = new ConcurrentHashMap<>();
    
    // Secondary indexes for efficient querying
    private final Map<String, Set<String>> vendorIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> instrumentIndex = new ConcurrentHashMap<>();

    @Override
    public void save(Price price) {
        String compositeKey = price.getCompositeKey();
        priceStore.put(compositeKey, price);
        
        // Update indexes
        vendorIndex.computeIfAbsent(price.getVendorId(), k -> ConcurrentHashMap.newKeySet())
                  .add(compositeKey);
        instrumentIndex.computeIfAbsent(price.getInstrumentId(), k -> ConcurrentHashMap.newKeySet())
                      .add(compositeKey);
    }

    @Override
    public Optional<Price> findByInstrumentAndVendor(String instrumentId, String vendorId) {
        String compositeKey = instrumentId + "_" + vendorId;
        return Optional.ofNullable(priceStore.get(compositeKey));
    }

    @Override
    public List<Price> findByVendor(String vendorId) {
        return vendorIndex.getOrDefault(vendorId, Collections.emptySet())
                         .stream()
                         .map(priceStore::get)
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
    }

    @Override
    public List<Price> findByInstrument(String instrumentId) {
        return instrumentIndex.getOrDefault(instrumentId, Collections.emptySet())
                             .stream()
                             .map(priceStore::get)
                             .filter(Objects::nonNull)
                             .collect(Collectors.toList());
    }

    @Override
    public List<Price> findAll() {
        return new ArrayList<>(priceStore.values());
    }

    @Override
    public void deleteOlderThan(LocalDateTime cutoffDate) {
        List<String> keysToRemove = priceStore.entrySet().stream()
                .filter(entry -> entry.getValue().getTimestamp().isBefore(cutoffDate))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        keysToRemove.forEach(key -> {
            Price price = priceStore.remove(key);
            if (price != null) {
                // Update indexes
                vendorIndex.getOrDefault(price.getVendorId(), Collections.emptySet()).remove(key);
                instrumentIndex.getOrDefault(price.getInstrumentId(), Collections.emptySet()).remove(key);
            }
        });
    }

    @Override
    public int count() {
        return priceStore.size();
    }

    @Override
    public void clear() {
        priceStore.clear();
        vendorIndex.clear();
        instrumentIndex.clear();
    }
}
