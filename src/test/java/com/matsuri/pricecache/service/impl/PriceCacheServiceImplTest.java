
package com.matsuri.pricecache.service.impl;

import com.matsuri.pricecache.domain.Price;
import com.matsuri.pricecache.repository.PriceRepository;
import com.matsuri.pricecache.service.PriceDistributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceCacheServiceImplTest {

    @Mock
    private PriceRepository priceRepository;
    
    @Mock
    private PriceDistributionService distributionService;

    private PriceCacheServiceImpl service;
    private Price testPrice;

    @BeforeEach
    void setUp() {
        service = new PriceCacheServiceImpl(priceRepository, distributionService);
        testPrice = new Price("AAPL", "VENDOR1", new BigDecimal("100.50"), 
                             new BigDecimal("100.60"), LocalDateTime.now(), "USD");
    }

    @Test
    void testPublishPrice() {
        service.publishPrice(testPrice);
        
        verify(priceRepository).save(testPrice);
        verify(distributionService).distributePrice(testPrice);
    }

    @Test
    void testGetPrice() {
        when(priceRepository.findByInstrumentAndVendor("AAPL", "VENDOR1"))
            .thenReturn(Optional.of(testPrice));
        
        Optional<Price> result = service.getPrice("AAPL", "VENDOR1");
        
        assertTrue(result.isPresent());
        assertEquals(testPrice, result.get());
        verify(priceRepository).findByInstrumentAndVendor("AAPL", "VENDOR1");
    }

    @Test
    void testGetPricesByVendor() {
        List<Price> expectedPrices = Arrays.asList(testPrice);
        when(priceRepository.findByVendor("VENDOR1")).thenReturn(expectedPrices);
        
        List<Price> result = service.getPricesByVendor("VENDOR1");
        
        assertEquals(expectedPrices, result);
        verify(priceRepository).findByVendor("VENDOR1");
    }

    @Test
    void testGetPricesByInstrument() {
        List<Price> expectedPrices = Arrays.asList(testPrice);
        when(priceRepository.findByInstrument("AAPL")).thenReturn(expectedPrices);
        
        List<Price> result = service.getPricesByInstrument("AAPL");
        
        assertEquals(expectedPrices, result);
        verify(priceRepository).findByInstrument("AAPL");
    }

    @Test
    void testCleanupOldPrices() {
        when(priceRepository.count()).thenReturn(10, 8);
        
        service.cleanupOldPrices();
        
        verify(priceRepository, times(2)).count();
        verify(priceRepository).deleteOlderThan(any(LocalDateTime.class));
    }

    @Test
    void testGetPriceCount() {
        when(priceRepository.count()).thenReturn(5);
        
        int result = service.getPriceCount();
        
        assertEquals(5, result);
        verify(priceRepository).count();
    }
}