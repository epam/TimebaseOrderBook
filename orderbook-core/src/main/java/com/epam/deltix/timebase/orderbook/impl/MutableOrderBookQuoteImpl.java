/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.timebase.orderbook.impl;

import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;

/**
 * @author Andrii_Ostapenko1
 */
class MutableOrderBookQuoteImpl implements MutableOrderBookQuote {
    /**
     * Ask, Bid or Trade price in primitive type.
     */
    protected double primitivePrice;
    /**
     * Ask, Bid or Trade price in decimal format.
     */
    @Decimal
    private long price;
    /**
     * Ask, Bid or Trade quantity.
     */
    @Decimal
    private long size;

    /**
     * Numbers of orders.
     */
    private long numberOfOrders;

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding.
     * see #getExchange()
     */
    private long exchangeId;

    @Override
    public long getExchangeId() {
        return exchangeId;
    }

    @Override
    public void setExchangeId(long exchangeId) {
        this.exchangeId = exchangeId;
    }

    @Override
    public boolean hasExchangeId() {
        return exchangeId != TypeConstants.EXCHANGE_NULL;
    }

    @Override
    @Decimal
    public long getPrice() {
        return price;
    }

    @Override
    public void setPrice(@Decimal final long price) {
        this.price = price;
        this.primitivePrice = Decimal64Utils.toDouble(price); // TODO Refactor
    }

    @Override
    public boolean hasPrice() {
        return price != TypeConstants.DECIMAL_NULL;
    }

    @Override
    @Decimal
    public long getSize() {
        return size;
    }

    @Override
    public void setSize(@Decimal final long size) {
        this.size = size;
    }

    @Override
    public boolean hasSize() {
        return size != TypeConstants.DECIMAL_NULL;
    }

    @Override
    public long getNumberOfOrders() {
        return numberOfOrders;
    }

    @Override
    public void setNumberOfOrders(long numberOfOrders) {
        this.numberOfOrders = numberOfOrders;
    }

    @Override
    public boolean hasNumberOfOrders() {
        return numberOfOrders != TypeConstants.INT64_NULL;
    }

    @Override
    public void copyFrom(final MutableOrderBookQuote src) {
        if (src instanceof MutableOrderBookQuoteImpl) {
            final MutableOrderBookQuoteImpl quote = (MutableOrderBookQuoteImpl) src;
            this.size = src.getSize();
            this.price = src.getPrice();
            this.exchangeId = src.getExchangeId();
            this.numberOfOrders = src.getNumberOfOrders();
            this.primitivePrice = quote.primitivePrice;
        }
    }

    @Override
    public void release() {
        this.size = TypeConstants.DECIMAL_NULL;
        this.price = TypeConstants.DECIMAL_NULL;
        this.exchangeId = TypeConstants.EXCHANGE_NULL;
        this.numberOfOrders = TypeConstants.INT64_NULL;
        this.primitivePrice = TypeConstants.INT64_NULL;
    }

    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder();
        return toString(str).toString();
    }

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
        if (hasNumberOfOrders()) {
            str.append(", \"numberOfOrders\": ").append(getNumberOfOrders());
        }
        if (hasExchangeId()) {
            str.append(", \"exchangeId\": ").append(AlphanumericUtils.toString(getExchangeId()));
        }
        str.append("}");
        return str;
    }

    @Override
    public int compareTo(final MutableOrderBookQuote o) {
        if (o instanceof MutableOrderBookQuoteImpl) {
            final MutableOrderBookQuoteImpl quote = (MutableOrderBookQuoteImpl) o;
            return Double.compare(primitivePrice, quote.primitivePrice);
        }
        return Decimal64Utils.compareTo(price, o.getPrice());
    }
}
