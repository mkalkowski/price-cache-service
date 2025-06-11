
package com.matsuri.pricecache.controller;

import com.matsuri.pricecache.domain.Price;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class PriceRequest {
    
    @NotBlank(message = "Instrument ID cannot be blank")
    private String instrumentId;
    
    @NotBlank(message = "Vendor ID cannot be blank")
    private String vendorId;
    
    @NotNull(message = "Bid price cannot be null")
    @DecimalMin(value = "0.0", message = "Bid price must be non-negative")
    private BigDecimal bidPrice;
    
    @NotNull(message = "Ask price cannot be null")
    @DecimalMin(value = "0.0", message = "Ask price must be non-negative")
    private BigDecimal askPrice;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @NotBlank(message = "Currency cannot be blank")
    private String currency;

    // Default constructor
    public PriceRequest() {}

    // Constructor
    public PriceRequest(String instrumentId, String vendorId, BigDecimal bidPrice, 
                       BigDecimal askPrice, LocalDateTime timestamp, String currency) {
        this.instrumentId = instrumentId;
        this.vendorId = vendorId;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.timestamp = timestamp;
        this.currency = currency;
    }

    public Price toPrice() {
        LocalDateTime priceTimestamp = timestamp != null ? timestamp : LocalDateTime.now();
        return new Price(instrumentId, vendorId, bidPrice, askPrice, priceTimestamp, currency);
    }

    // Getters and Setters
    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }
    
    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }
    
    public BigDecimal getBidPrice() { return bidPrice; }
    public void setBidPrice(BigDecimal bidPrice) { this.bidPrice = bidPrice; }
    
    public BigDecimal getAskPrice() { return askPrice; }
    public void setAskPrice(BigDecimal askPrice) { this.askPrice = askPrice; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}