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
package com.epam.deltix.orderbook.benchmark;

import com.epam.deltix.orderbook.core.api.OrderBook;
import com.epam.deltix.orderbook.core.api.OrderBookFactory;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.orderbook.core.options.OrderBookOptions;
import com.epam.deltix.orderbook.core.options.OrderBookOptionsBuilder;
import com.epam.deltix.orderbook.core.options.OrderBookType;
import com.epam.deltix.orderbook.core.options.UpdateMode;
import com.epam.deltix.timebase.messages.universal.DataModelType;
import com.epam.deltix.timebase.messages.universal.PackageHeader;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 5, time = 5)
public class ConsolidateOrderBookVendorSnapshotUpdateBenchmark extends AbstractOrderBookBenchmarkAll {

    private static final String SYMBOL_BTS = "BTS";

    @Param({"1", "3"})
    private int numberOfExchange;
    @Param({"40", "1000"})
    private int maxDepth;

    final OrderBookOptions opt = new OrderBookOptionsBuilder()
            .symbol(SYMBOL_BTS)
            .orderBookType(OrderBookType.CONSOLIDATED)
            .quoteLevels(DataModelType.LEVEL_TWO)
            .initialDepth(maxDepth)
            .initialExchangesPoolSize(numberOfExchange)
            .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
            .build();

    private final OrderBook<OrderBookQuote> orderBook = OrderBookFactory.create(opt);

    public static void main(final String[] args) throws RunnerException {
        final Options opt = new OptionsBuilder().include(ConsolidateOrderBookVendorSnapshotUpdateBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }

    @Setup(value = Level.Invocation)
    public void setUpVendorUpdate() {
        createVendorUpdate(maxDepth, numberOfExchange, SYMBOL_BTS);
    }

    @Setup(value = Level.Iteration)
    public void showOrderBookSize() {
        totalSize(orderBook);
    }

    @Benchmark
    public PackageHeader vendorSnapshot() {
        orderBook.update(packageHeader);
        return packageHeader;
    }

}
