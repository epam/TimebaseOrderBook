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
package com.epam.deltix.orderbook.core.impl;

import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.containers.BinaryAsciiString;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.BasePriceEntryInfo;
import com.epam.deltix.timebase.messages.universal.PackageHeaderInfo;
import com.epam.deltix.util.annotations.Alphanumeric;


/**
 * @author Andrii_Ostapenko1
 */
class MutableOrderBookQuoteL3Impl implements MutableOrderBookQuote {

    /**
     * Ask, Bid or Trade price in decimal format.
     */
    @Decimal
    private long price = TypeConstants.DECIMAL_NULL;
    /**
     * Ask, Bid or Trade quantity.
     */
    @Decimal
    private long size = TypeConstants.DECIMAL_NULL;

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding.
     * see #getExchange()
     */
    @Alphanumeric
    private long exchangeId = TypeConstants.EXCHANGE_NULL;

    /**
     * Quote ID. In Forex market, for example, quote ID can be referenced in
     * TradeOrders (to identify market maker's quote/rate we want to deal with).
     * Each market maker usually keeps this ID unique per session per day. This
     * is a alpha-numeric text text field that can reach 64 characters or more,
     */
    private final BinaryAsciiString quoteId = new BinaryAsciiString(32);

    private static final BinaryAsciiString DEFAULT_PARTICIPANT_ID = new BinaryAsciiString(0);

    /**
     * Id of participant (or broker ID).
     */
    private BinaryAsciiString participantId = DEFAULT_PARTICIPANT_ID;

    /**
     * Sequence number of a quote, required for chronological ordering
     */
    private long sequenceNumber;

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
    public CharSequence getQuoteId() {
        return quoteId;
    }

    @Override
    public boolean hasQuoteId() {
        return !this.quoteId.isEmpty();
    }

    @Override
    public CharSequence getParticipantId() {
        return participantId;
    }

    @Override
    public boolean hasParticipantId() {
        return !participantId.isEmpty();
    }

    @Override
    public long getNumberOfOrders() {
        return 1;
    }

    @Override
    public void setNumberOfOrders(long numberOfOrders) {
        // do nothing or unsupported operation
    }

    @Override
    public boolean hasNumberOfOrders() {
        return false;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public void copyFrom(final MutableOrderBookQuote src) {
        if (src == null) {
            return;
        }
        this.size = src.getSize();
        this.price = src.getPrice();
        this.exchangeId = src.getExchangeId();
        copyFrom(src.getQuoteId(), this.quoteId);
        copyFromParticipantId(src.getParticipantId());
    }

    @Override
    public void copyFrom(final PackageHeaderInfo pck, final BasePriceEntryInfo src) {
        assert src != null;
        this.size = src.getSize();
        this.price = src.getPrice();
        this.exchangeId = src.getExchangeId();
        copyFrom(src.getQuoteId(), this.quoteId);
        copyFromParticipantId(src.getParticipantId());
    }

    @Override
    public void release() {
        this.size = TypeConstants.DECIMAL_NULL;
        this.price = TypeConstants.DECIMAL_NULL;
        this.exchangeId = TypeConstants.EXCHANGE_NULL;
        this.quoteId.clear();
        this.participantId.clear();
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
        if (hasQuoteId()) {
            str.append(", \"quoteId\": ").append(getQuoteId());
        }
        if (hasExchangeId()) {
            str.append(", \"exchangeId\": ").append(AlphanumericUtils.toString(getExchangeId()));
        }
        str.append("}");
        return str;
    }

    @Override
    public int compareTo(final MutableOrderBookQuote o) {
        return Decimal64Utils.compareTo(price, o.getPrice());
    }

    private void copyFrom(final CharSequence src, final BinaryAsciiString dst) {
        if (src != null) {
            dst.assign(src);
        } else {
            dst.clear();
        }
    }

    private void copyFromParticipantId(final CharSequence src) {
        if (src != null) {
            if (this.participantId == DEFAULT_PARTICIPANT_ID) {
                this.participantId = new BinaryAsciiString(32);
            }
            this.participantId.assign(src);
        } else {
            this.participantId.clear();
        }
    }
}
