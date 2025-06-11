package com.matsuri.pricecache.controller;

import com.matsuri.pricecache.domain.Price;
import com.matsuri.pricecache.service.PriceCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PriceController.class)
class PriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceCacheService priceCacheService;

    private ObjectMapper objectMapper;
    private Price testPrice;
    private PriceRequest testRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        LocalDateTime now = LocalDateTime.now();
        testPrice = new Price("AAPL", "VENDOR1", new BigDecimal("100.50"), 
                             new BigDecimal("100.60"), now, "USD");
        testRequest = new PriceRequest("AAPL", "VENDOR1", new BigDecimal("100.50"), 
                                      new BigDecimal("100.60"), now, "USD");
    }

    @Test
    void testPublishPrice() throws Exception {
        mockMvc.perform(post("/api/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Price published successfully"));

        verify(priceCacheService).publishPrice(any(Price.class));
    }

    @Test
    void testGetPrice_Found() throws Exception {
        when(priceCacheService.getPrice("AAPL", "VENDOR1"))
            .thenReturn(Optional.of(testPrice));

        mockMvc.perform(get("/api/prices/AAPL/VENDOR1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instrumentId").value("AAPL"))
                .andExpect(jsonPath("$.vendorId").value("VENDOR1"));

        verify(priceCacheService).getPrice("AAPL", "VENDOR1");
    }

    @Test
    void testGetPrice_NotFound() throws Exception {
        when(priceCacheService.getPrice("AAPL", "VENDOR1"))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/prices/AAPL/VENDOR1"))
                .andExpect(status().isNotFound());

        verify(priceCacheService).getPrice("AAPL", "VENDOR1");
    }

    @Test
    void testGetPricesByVendor() throws Exception {
        when(priceCacheService.getPricesByVendor("VENDOR1"))
            .thenReturn(Arrays.asList(testPrice));

        mockMvc.perform(get("/api/prices/vendor/VENDOR1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].vendorId").value("VENDOR1"));

        verify(priceCacheService).getPricesByVendor("VENDOR1");
    }

    @Test
    void testCleanupOldPrices() throws Exception {
        mockMvc.perform(post("/api/prices/cleanup"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cleanup completed successfully"));

        verify(priceCacheService).cleanupOldPrices();
    }
}