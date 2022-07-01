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

import com.epam.deltix.orderbook.core.api.Exchange;
import com.epam.deltix.orderbook.core.options.Option;

import java.util.*;

/**
 * @author Andrii_Ostapenko1
 */
class MutableExchangeListImpl<Quote, StockExchange extends Exchange<Quote>>
        implements MutableExchangeList<StockExchange> {

    private static final int DEFAULT_INITIAL_CAPACITY = 1;

    private final List<Option<StockExchange>> data;

    private final ReusableIterator<StockExchange> itr;

    MutableExchangeListImpl() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    MutableExchangeListImpl(final int initialCapacity) {
        this.data = new ArrayList<>(initialCapacity);
        this.itr = new ReusableIterator<>(data);
    }

    @Override
    public void add(final StockExchange exchange) {
        Objects.requireNonNull(exchange);
        data.add(Option.wrap(exchange));
    }

    @Override
    public Option<StockExchange> getById(final long exchangeId) {
        for (int i = 0; i < data.size(); i++) {
            final Option<StockExchange> exchange = data.get(i);
            if (exchange.get().getExchangeId() == exchangeId) {
                return exchange;
            }
        }
        return Option.empty();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public Iterator<StockExchange> iterator() {
        itr.reset();
        return itr;
    }

    static final class ReusableIterator<StockExchange> implements Iterator<StockExchange> {

        private final List<Option<StockExchange>> data;

        private short cursor;

        ReusableIterator(final List<Option<StockExchange>> data) {
            this.data = data;
        }

        private void reset() {
            cursor = 0;
        }

        @Override
        public boolean hasNext() {
            return cursor != data.size();
        }

        @Override
        public StockExchange next() {
            try {
                return data.get(cursor++).get();
            } catch (final IndexOutOfBoundsException ignore) {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only iterator");
        }
    }
}
