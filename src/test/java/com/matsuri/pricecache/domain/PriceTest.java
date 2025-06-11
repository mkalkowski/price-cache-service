package com.matsuri.pricecache.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PriceTest {

    @Test
    void testValidPriceCreation() {
        LocalDateTime now = LocalDateTime.now();
        Price price = new Price("AAPL", "VENDOR1", 
                               new BigDecimal("100.50"), new BigDecimal("100.60"), 
                               now, "USD");

        assertEquals("AAPL", price.getInstrumentId());
        assertEquals("VENDOR1", price.getVendorId());
        assertEquals(new BigDecimal("100.50"), price.getBidPrice());
        assertEquals(new BigDecimal("100.60"), price.getAskPrice());
        assertEquals(now, price.getTimestamp());
        assertEquals("USD", price.getCurrency());
        assertEquals("AAPL_VENDOR1", price.getCompositeKey());
    }

    @Test
    void testInvalidPriceCreation_NullValues() {
        LocalDateTime now = LocalDateTime.now();
        
        assertThrows(NullPointerException.class, () -> 
            new Price(null, "VENDOR1", new BigDecimal("100.50"), 
                      new BigDecimal("100.60"), now, "USD"));
        
        assertThrows(NullPointerException.class, () -> 
            new Price("AAPL", null, new BigDecimal("100.50"), 
                      new BigDecimal("100.60"), now, "USD"));
    }

    @Test
    void testInvalidPriceCreation_NegativePrices() {
        LocalDateTime now = LocalDateTime.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Price("AAPL", "VENDOR1", new BigDecimal("-100.50"), 
                      new BigDecimal("100.60"), now, "USD"));
    }

    @Test
    void testInvalidPriceCreation_BidHigherThanAsk() {
        LocalDateTime now = LocalDateTime.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Price("AAPL", "VENDOR1", new BigDecimal("100.70"), 
                      new BigDecimal("100.60"), now, "USD"));
    }

    @Test
    void testPriceEquality() {
        LocalDateTime now = LocalDateTime.now();
        Price price1 = new Price("AAPL", "VENDOR1", new BigDecimal("100.50"), 
                                new BigDecimal("100.60"), now, "USD");
        Price price2 = new Price("AAPL", "VENDOR1", new BigDecimal("100.50"), 
                                new BigDecimal("100.60"), now, "USD");

        assertEquals(price1, price2);
        assertEquals(price1.hashCode(), price2.hashCode());
    }
}