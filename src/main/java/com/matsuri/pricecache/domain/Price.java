
package com.matsuri.pricecache.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a price quote for a traded instrument from a specific vendor.
 * This is the core domain object that encapsulates all price-related information.
 */
public class Price {
    private final String instrumentId;
    private final String vendorId;
    private final BigDecimal bidPrice;
    private final BigDecimal askPrice;
    private final LocalDateTime timestamp;
    private final String currency;

    public Price(String instrumentId, String vendorId, BigDecimal bidPrice, 
                 BigDecimal askPrice, LocalDateTime timestamp, String currency) {
        this.instrumentId = Objects.requireNonNull(instrumentId, "Instrument ID cannot be null");
        this.vendorId = Objects.requireNonNull(vendorId, "Vendor ID cannot be null");
        this.bidPrice = Objects.requireNonNull(bidPrice, "Bid price cannot be null");
        this.askPrice = Objects.requireNonNull(askPrice, "Ask price cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        
        if (bidPrice.compareTo(BigDecimal.ZERO) < 0 || askPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Prices cannot be negative");
        }
        if (bidPrice.compareTo(askPrice) > 0) {
            throw new IllegalArgumentException("Bid price cannot be higher than ask price");
        }
    }

    // Getters
    public String getInstrumentId() { return instrumentId; }
    public String getVendorId() { return vendorId; }
    public BigDecimal getBidPrice() { return bidPrice; }
    public BigDecimal getAskPrice() { return askPrice; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCurrency() { return currency; }

    public String getCompositeKey() {
        return instrumentId + "_" + vendorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Price price = (Price) o;
        return Objects.equals(instrumentId, price.instrumentId) &&
               Objects.equals(vendorId, price.vendorId) &&
               Objects.equals(bidPrice, price.bidPrice) &&
               Objects.equals(askPrice, price.askPrice) &&
               Objects.equals(timestamp, price.timestamp) &&
               Objects.equals(currency, price.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrumentId, vendorId, bidPrice, askPrice, timestamp, currency);
    }

    @Override
    public String toString() {
        return String.format("Price{instrument='%s', vendor='%s', bid=%s, ask=%s, time=%s, currency='%s'}",
                instrumentId, vendorId, bidPrice, askPrice, timestamp, currency);
    }
}
