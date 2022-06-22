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
package com.epam.deltix.timebase.orderbook;

import com.epam.deltix.timebase.messages.universal.DataModelType;
import com.epam.deltix.timebase.messages.universal.PackageHeader;
import com.epam.deltix.timebase.orderbook.api.OrderBook;
import com.epam.deltix.timebase.orderbook.api.OrderBookFactory;
import com.epam.deltix.timebase.orderbook.api.OrderBookQuote;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptions;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptionsBuilder;
import com.epam.deltix.timebase.orderbook.options.OrderBookType;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 5, time = 5)
public class SingleExchangeOrderBookIncrementalUpdateBenchmark extends AbstractOrderBookBenchmarkAll {

    private static final String SYMBOL_BTS = "BTS";

    @Param({"1"})
    private int numberOfExchange;
    @Param({"40", "1000"})
    private int maxDepth;

    final OrderBookOptions opt = new OrderBookOptionsBuilder()
            .symbol(SYMBOL_BTS)
            .orderBookType(OrderBookType.SINGLE_EXCHANGE)
            .quoteLevels(DataModelType.LEVEL_TWO)
            .initialDepth(maxDepth)
            .initialExchangesPoolSize(numberOfExchange)
            .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
            .build();

    private final OrderBook<OrderBookQuote> orderBook = OrderBookFactory.create(opt);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(SingleExchangeOrderBookIncrementalUpdateBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }

    @Setup
    public void initUpdate() {
        initUpdateMarketData(numberOfExchange, maxDepth);
    }

    @Setup(value = Level.Iteration)
    public void setUpVendorUpdate() {
        IntStream.range(0, numberOfExchange)
                .forEach(exchangeId -> orderBook.update(createVendorUpdate(maxDepth, exchangeId, SYMBOL_BTS)));
        totalSize(orderBook);
    }

    @Setup(value = Level.Invocation)
    public void setUp() {
        generateUpdateMarketDate(maxDepth, numberOfExchange, SYMBOL_BTS);
    }

    @Benchmark
    public PackageHeader randomIncrementalUpdate() {
        orderBook.update(packageHeader);
        return packageHeader;
    }

}
