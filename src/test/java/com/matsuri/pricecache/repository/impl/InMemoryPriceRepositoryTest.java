package com.matsuri.pricecache.repository.impl;

import com.matsuri.pricecache.domain.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryPriceRepositoryTest {

    private InMemoryPriceRepository repository;
    private Price testPrice1;
    private Price testPrice2;
    private Price testPrice3;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPriceRepository();
        LocalDateTime now = LocalDateTime.now();
        
        testPrice1 = new Price("AAPL", "VENDOR1", new BigDecimal("100.50"), 
                              new BigDecimal("100.60"), now, "USD");
        testPrice2 = new Price("AAPL", "VENDOR2", new BigDecimal("100.45"), 
                              new BigDecimal("100.55"), now, "USD");
        testPrice3 = new Price("GOOGL", "VENDOR1", new BigDecimal("2500.00"), 
                              new BigDecimal("2500.50"), now, "USD");
    }

    @Test
    void testSaveAndFind() {
        repository.save(testPrice1);
        
        Optional<Price> found = repository.findByInstrumentAndVendor("AAPL", "VENDOR1");
        assertTrue(found.isPresent());
        assertEquals(testPrice1, found.get());
    }

    @Test
    void testFindByVendor() {
        repository.save(testPrice1);
        repository.save(testPrice3);
        
        List<Price> prices = repository.findByVendor("VENDOR1");
        assertEquals(2, prices.size());
        assertTrue(prices.contains(testPrice1));
        assertTrue(prices.contains(testPrice3));
    }

    @Test
    void testFindByInstrument() {
        repository.save(testPrice1);
        repository.save(testPrice2);
        
        List<Price> prices = repository.findByInstrument("AAPL");
        assertEquals(2, prices.size());
        assertTrue(prices.contains(testPrice1));
        assertTrue(prices.contains(testPrice2));
    }

    @Test
    void testDeleteOlderThan() {
        LocalDateTime old = LocalDateTime.now().minusDays(35);
        LocalDateTime recent = LocalDateTime.now().minusDays(5);
        
        Price oldPrice = new Price("OLD", "VENDOR1", new BigDecimal("100.00"), 
                                  new BigDecimal("100.10"), old, "USD");
        Price recentPrice = new Price("NEW", "VENDOR1", new BigDecimal("200.00"), 
                                     new BigDecimal("200.10"), recent, "USD");
        
        repository.save(oldPrice);
        repository.save(recentPrice);
        
        assertEquals(2, repository.count());
        
        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));
        
        assertEquals(1, repository.count());
        assertTrue(repository.findByInstrumentAndVendor("NEW", "VENDOR1").isPresent());
        assertFalse(repository.findByInstrumentAndVendor("OLD", "VENDOR1").isPresent());
    }

    @Test
    void testClear() {
        repository.save(testPrice1);
        repository.save(testPrice2);
        assertEquals(2, repository.count());
        
        repository.clear();
        assertEquals(0, repository.count());
    }
}