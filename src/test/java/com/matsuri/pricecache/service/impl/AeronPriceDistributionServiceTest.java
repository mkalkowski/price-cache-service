package com.matsuri.pricecache.service.impl;

import com.matsuri.pricecache.domain.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AeronPriceDistributionServiceTest {

    private AeronPriceDistributionService service;

    @BeforeEach
    void setUp() {
        service = new AeronPriceDistributionService();
        ReflectionTestUtils.setField(service, "channel", "aeron:udp?endpoint=localhost:40123");
        ReflectionTestUtils.setField(service, "streamId", 1001);

        Price testPrice = new Price("AAPL", "VENDOR1", new BigDecimal("100.50"),
                new BigDecimal("100.60"), LocalDateTime.now(), "USD");
    }

    @Test
    void testDistributePriceWithoutConnection() {
        assertDoesNotThrow(() -> service.distributePrice(null));
    }
}