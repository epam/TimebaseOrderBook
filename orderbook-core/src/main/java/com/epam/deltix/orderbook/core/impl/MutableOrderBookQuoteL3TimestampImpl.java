package com.epam.deltix.orderbook.core.impl;

import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.universal.BasePriceEntryInfo;
import com.epam.deltix.timebase.messages.universal.PackageHeaderInfo;


/**
 * @author Andrii_Ostapenko1
 */
class MutableOrderBookQuoteL3TimestampImpl extends MutableOrderBookQuoteL3Impl {

    protected long originalTimestamp = TIMESTAMP_UNKNOWN;
    protected long timestamp = TIMESTAMP_UNKNOWN;

    @Override
    public void setOriginalTimestamp(final long timestamp) {
        this.originalTimestamp = timestamp;
    }

    @Override
    public long getOriginalTimestamp() {
        return originalTimestamp;
    }

    @Override
    public boolean hasOriginalTimestamp() {
        return originalTimestamp != TIMESTAMP_UNKNOWN;
    }

    @Override
    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean hasTimestamp() {
        return timestamp != TIMESTAMP_UNKNOWN;
    }

    @Override
    public void copyFrom(final MutableOrderBookQuote src) {
        super.copyFrom(src);
        this.originalTimestamp = src.getOriginalTimestamp();
        this.timestamp = src.getTimestamp();
    }

    @Override
    public void copyFrom(final PackageHeaderInfo pck, final BasePriceEntryInfo src) {
        super.copyFrom(pck, src);
        this.originalTimestamp = pck.getOriginalTimestamp();
        this.timestamp = pck.getTimeStampMs();
    }

    @Override
    public void release() {
        super.release();
        this.originalTimestamp = TIMESTAMP_UNKNOWN;
        this.timestamp = TIMESTAMP_UNKNOWN;
    }

    @Override
    public StringBuilder toString(final StringBuilder str) {
        str.append("{ \"$type\":  \"Quote\"");
        str.append("{ \"hashcode\": ").append(this.hashCode());
        if (hasPrice()) {
            str.append(", \"price\": ");
            Decimal64Utils.appendTo(getPrice(), str);
        }
        if (hasSize()) {
            str.append(", \"size\": ");
            Decimal64Utils.appendTo(getSize(), str);
        }
        if (hasQuoteId()) {
            str.append(", \"quoteId\": ").append(getQuoteId());
        }
        if (hasExchangeId()) {
            str.append(", \"exchangeId\": ").append(AlphanumericUtils.toString(getExchangeId()));
        }
        if (hasOriginalTimestamp()) {
            str.append(", \"originalTimestamp\": ").append(getOriginalTimestamp());
        }
        if (hasTimestamp()) {
            str.append(", \"timestamp\": ").append(getTimestamp());
        }
        str.append("}");
        return str;
    }
}
