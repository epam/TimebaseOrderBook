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
import com.epam.deltix.orderbook.core.api.MarketSide;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.util.annotations.Alphanumeric;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * @author Andrii_Ostapenko1
 */
public class MutableExchangeImpl<Quote, Processor extends QuoteProcessor<Quote>>
        implements MutableExchange<Quote, Processor> {

    @Alphanumeric
    private final long exchangeId;
    private final Processor processor;

    public MutableExchangeImpl(@Alphanumeric final long exchangeId,
                               final Processor processor) {
        Objects.requireNonNull(processor);
        this.exchangeId = exchangeId;
        this.processor = processor;
    }

    @Override
    @Alphanumeric
    public long getExchangeId() {
        return exchangeId;
    }

    @Override
    public MarketSide<Quote> getMarketSide(final QuoteSide side) {
        return processor.getMarketSide(side);
    }

    @Override
    public Processor getProcessor() {
        return processor;
    }

    @Override
    public boolean isWaitingForSnapshot() {
        return processor.isWaitingForSnapshot();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MutableExchangeImpl.class.getSimpleName() + "[", "]")
                .add("exchangeId=" + AlphanumericUtils.toString(exchangeId))
                .add("aks=" + processor.getMarketSide(QuoteSide.ASK).depth())
                .add("bid=" + processor.getMarketSide(QuoteSide.BID).depth())
                .toString();
    }
}
