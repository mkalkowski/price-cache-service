
package com.matsuri.pricecache.controller;

import com.matsuri.pricecache.domain.Price;
import com.matsuri.pricecache.service.PriceCacheService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST API controller for price cache operations.
 * Provides endpoints for publishing and retrieving price data.
 */
@RestController
@RequestMapping("/api/prices")
public class PriceController {
    
    private final PriceCacheService priceCacheService;

    @Autowired
    public PriceController(PriceCacheService priceCacheService) {
        this.priceCacheService = priceCacheService;
    }

    @PostMapping
    @Operation(summary = "Price publication")
    public ResponseEntity<String> publishPrice(@Valid @RequestBody PriceRequest request) {
        Price price = request.toPrice();
        priceCacheService.publishPrice(price);
        return ResponseEntity.status(HttpStatus.CREATED).body("Price published successfully");
    }

    @Operation(summary = "Price retrieval")
    @GetMapping(value = "/{instrumentId}/{vendorId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Price> getPrice(@PathVariable String instrumentId, 
                                         @PathVariable String vendorId) {
        Optional<Price> price = priceCacheService.getPrice(instrumentId, vendorId);
        return price.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all prices by vendor")
    @GetMapping(value= "/vendor/{vendorId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Price>> getPricesByVendor(@PathVariable String vendorId) {
        List<Price> prices = priceCacheService.getPricesByVendor(vendorId);
        return ResponseEntity.ok(prices);
    }

    @Operation(summary = "Price retrieval by instrument")
    @GetMapping(value = "/instrument/{instrumentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Price>> getPricesByInstrument(@PathVariable String instrumentId) {
        List<Price> prices = priceCacheService.getPricesByInstrument(instrumentId);
        return ResponseEntity.ok(prices);
    }

    @Operation(summary = "Price retrieval in bulk")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Price>> getAllPrices() {
        List<Price> prices = priceCacheService.getAllPrices();
        return ResponseEntity.ok(prices);
    }

    @Operation(summary = "Clean-up data repository")
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupOldPrices() {
        priceCacheService.cleanupOldPrices();
        return ResponseEntity.ok("Cleanup completed successfully");
    }

    @Operation(summary = "Return the number of prices held")
    @GetMapping("/count")
    public ResponseEntity<Integer> getPriceCount() {
        int count = priceCacheService.getPriceCount();
        return ResponseEntity.ok(count);
    }
}
