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

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.options.GapMode;
import com.epam.deltix.orderbook.core.options.UnreachableDepthMode;
import com.epam.deltix.orderbook.core.options.UpdateMode;
import com.epam.deltix.timebase.messages.universal.L2EntryUpdateInfo;
import com.epam.deltix.timebase.messages.universal.QuoteSide;

import java.util.StringJoiner;

/**
 * @author Andrii_Ostapenko1
 */
class L2ConsolidatedQuoteProcessor<Quote extends MutableOrderBookQuote> extends AbstractL2MultiExchangeProcessor<Quote> {

    L2ConsolidatedQuoteProcessor(final int initialExchangeCount,
                                 final int initialDepth,
                                 final int maxDepth,
                                 final ObjectPool<Quote> pool,
                                 final GapMode gapMode,
                                 final UpdateMode updateMode,
                                 final UnreachableDepthMode unreachableDepthMode) {
        super(initialExchangeCount, initialDepth, maxDepth, pool, gapMode, updateMode,unreachableDepthMode);
    }

    @Override
    public void updateQuote(final Quote previous, final QuoteSide side, final L2EntryUpdateInfo update) {
        // Ignore
    }

    @Override
    public String getDescription() {
        return "L2/Consolidation of multiple exchanges";
    }

    @Override
    public void clear() {
        asks.clear();
        bids.clear();
        for (final MutableExchange<Quote, L2Processor<Quote>> exchange : this.getExchanges()) {
            exchange.getProcessor().clear();
        }
    }

    @Override
    public boolean removeQuote(final Quote remove, final L2MarketSide<Quote> marketSide) {
        final short level = marketSide.binarySearchLevelByPrice(remove);
        if (level != L2MarketSide.NOT_FOUND) {
            if (remove.getExchangeId() == marketSide.getQuote(level).getExchangeId() &&
                    Decimal64Utils.equals(remove.getPrice(), marketSide.getQuote(level).getPrice())) {
                marketSide.remove(level);
                return true;
            } else {
                final int size = exchanges.size();
                for (int i = 0, k = level + i; i < size; i++, k = level + i) {
                    if (marketSide.hasLevel((short) (k))) {
                        if (remove.getExchangeId() == marketSide.getQuote(k).getExchangeId() &&
                                Decimal64Utils.equals(remove.getPrice(), marketSide.getQuote(k).getPrice())) {
                            marketSide.remove(k);
                            return true;
                        }
                    }
                }

                for (int i = 0, k = level - i; i < size; i++, k = level - i) {
                    if (marketSide.hasLevel((short) (k))) {
                        if (remove.getExchangeId() == marketSide.getQuote(k).getExchangeId() &&
                                Decimal64Utils.equals(remove.getPrice(), marketSide.getQuote(k).getPrice())) {
                            marketSide.remove(k);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Quote insertQuote(final Quote insert, final L2MarketSide<Quote> marketSide) {
        final short level = marketSide.binarySearchNextLevelByPrice(insert);
        marketSide.add(level, insert);
        return insert;
    }

    @Override
    public L2Processor<Quote> clearExchange(final L2Processor<Quote> exchange) {
        removeAll(exchange, QuoteSide.ASK);
        removeAll(exchange, QuoteSide.BID);
        exchange.clear();
        return exchange;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", L2ConsolidatedQuoteProcessor.class.getSimpleName() + "[", "]")
                .add("exchanges=" + exchanges.size())
                .add("bids=" + bids.depth())
                .add("asks=" + asks.depth())
                .toString();
    }
}
