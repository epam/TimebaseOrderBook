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

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.MarketMessageInfo;
import com.epam.deltix.timebase.messages.universal.DataModelType;
import com.epam.deltix.timebase.messages.universal.PackageHeaderInfo;
import com.epam.deltix.timebase.orderbook.api.OrderBook;
import com.epam.deltix.timebase.orderbook.api.OrderBookFactory;
import com.epam.deltix.timebase.orderbook.api.OrderBookQuote;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptions;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptionsBuilder;
import com.epam.deltix.timebase.orderbook.options.OrderBookType;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;
import com.epam.deltix.util.cmdline.DefaultApplication;
import org.openjdk.jol.info.GraphLayout;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * @author Andrii_Ostapenko1
 */
public class OrderBookIT extends DefaultApplication {

    private static final Log LOGGER = LogFactory.getLog(OrderBookIT.class);

    public OrderBookIT(String... args) {
        super(args);
    }

    private static double readStream(DXTickStream stream, String symbol, long startReadTime, long endReadTime, Consumer<MarketMessageInfo> consumer) {
        LOGGER.info().append("Start reading stream ").append(stream.getKey())
                .append("; Symbol: ").append(symbol)
                .append("; Start time: ").append(startReadTime == Long.MIN_VALUE ? "min" : Instant.ofEpochMilli(startReadTime))
                .append("; End time: ").append(endReadTime == Long.MAX_VALUE ? "max" : Instant.ofEpochMilli(endReadTime)).commit();

        try (TickCursor cursor = stream.select(startReadTime, new SelectionOptions(), null, new CharSequence[]{symbol})) {
            if (!cursor.next()) {
                LOGGER.info().append("Empty stream").commit();
                return -1;
            }

            long msgCount = 0;
            long entriesCount = 0;
            long startTime = System.currentTimeMillis();
            long streamTime = cursor.getMessage().getTimeStampMs();
            long prevStreamTime = streamTime;
            while (cursor.next()) {
                InstrumentMessage message = cursor.getMessage();
                if (message.getTimeStampMs() >= endReadTime) {
                    break;
                }

                if (message.getTimeStampMs() - prevStreamTime > 30_000) {
                    LOGGER.info("Empty interval: " + message.getTimeString() + "; " + message.getTimeStampMs());
                }

                if (message instanceof MarketMessageInfo) {
                    msgCount++;
                    if (message instanceof PackageHeaderInfo) {
                        PackageHeaderInfo packageHeader = (PackageHeaderInfo) message;
                        entriesCount += packageHeader.getEntries().size();
                    }
                    consumer.accept((MarketMessageInfo) message);

                    if (msgCount % 1000000 == 0) {
                        logTime(startTime, prevStreamTime - streamTime, msgCount, entriesCount);
                    }
                }

                prevStreamTime = message.getTimeStampMs();
            }

            logTime(startTime, prevStreamTime - startReadTime, msgCount, entriesCount);

            long timePass = System.currentTimeMillis() - startTime;
            double timeSeconds = (double) timePass / 1000.0;
            return (msgCount / timeSeconds);
        }
    }

    private static void logTime(long startTime, long streamTime, long msgCount, long entriesCount) {
        long timePass = System.currentTimeMillis() - startTime;
        double timeSeconds = (double) timePass / 1000.0;
        double sTimeMinutes = streamTime / (60 * 1000.0);
        LOGGER.info(
                "Read msgCount = " + msgCount +
                        "; entriesCount = " + entriesCount +
                        "; read time = " + (timeSeconds) +
                        "; stream time (m) = " + sTimeMinutes +
                        "; msg/s = " + (msgCount / timeSeconds) +
                        "; entries/s = " + (entriesCount / timeSeconds) +
                        "; time(m)/s = " + (sTimeMinutes / timeSeconds)
        );
    }

    public static void main(String[] args) {
        new OrderBookIT(args).start();
    }

    @Override
    public void run() throws Throwable {
        final String timebaseUrl = getArgValue("-url", "dxtick://localhost:8011");
        final String timebaseUser = getArgValue("-user");
        final String timebasePassword = getArgValue("-password");
        final String streamKey = getArgValue("-stream", "bitmex");
        final String symbol = getArgValue("-symbol", "BTC/USD");

        long startTime = getLongArgValue("-start_time_ms", Long.MIN_VALUE);
        String startTimeStr = getArgValue("-start_time");
        if (startTimeStr != null) {
            startTime = Instant.parse(startTimeStr).toEpochMilli();
        }

        long endTime = getLongArgValue("-end_time_ms", Long.MAX_VALUE);
        String endTimeStr = getArgValue("-end_time");
        if (endTimeStr != null) {
            endTime = Instant.parse(endTimeStr).toEpochMilli();
        }

        DXTickDB db = timebaseUser != null ?
                TickDBFactory.createFromUrl(timebaseUrl, timebaseUser, timebasePassword) :
                TickDBFactory.createFromUrl(timebaseUrl);
        db.open(true);
        try {
            DXTickStream stream = db.getStream(streamKey);
            if (stream == null) {
                throw new RuntimeException("Can't find stream " + streamKey);
            }
            long[] range = stream.getTimeRange();
            if (startTime == Long.MIN_VALUE) {
                startTime = range[0];
            }

            final StringBuilder resultBuilder = new StringBuilder();

            LOGGER.info().append("Warmup pass...").commit();
            resultBuilder.append(executed(symbol, startTime, startTime + 60 * 60 * 1000, stream, p -> {
            }, "Black Hole", new Object()));
            LOGGER.info().append("Main pass...").commit();

            final OrderBookOptions symbolOptions = new OrderBookOptionsBuilder()
                    .symbol(symbol)
                    .build();

            final OrderBookOptions l1SingleExchangeOptions = new OrderBookOptionsBuilder()
                    .parent(symbolOptions)
                    .orderBookType(OrderBookType.SINGLE_EXCHANGE)
                    .quoteLevels(DataModelType.LEVEL_ONE)
                    .initialDepth(500)
                    .initialExchangesPoolSize(1)
                    .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
                    .build();

            final OrderBook<OrderBookQuote> singleExchangeL1Book = OrderBookFactory.create(l1SingleExchangeOptions);
            resultBuilder.append(executed(symbol, startTime, endTime, stream, singleExchangeL1Book::update, singleExchangeL1Book.getDescription(), singleExchangeL1Book));
            //
            final OrderBookOptions l2CommonOptions = new OrderBookOptionsBuilder()
                    .parent(symbolOptions)
                    .quoteLevels(DataModelType.LEVEL_TWO)
                    .initialDepth(40)
                    .initialExchangesPoolSize(1)
                    .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
                    .build();

            final OrderBookOptions l2ConsolidatedOptions = new OrderBookOptionsBuilder()
                    .parent(l2CommonOptions)
                    .orderBookType(OrderBookType.CONSOLIDATED)
                    .build();

            final OrderBook<OrderBookQuote> consolidatedBook = OrderBookFactory.create(l2ConsolidatedOptions);
            resultBuilder.append(executed(symbol, startTime, endTime, stream, consolidatedBook::update, consolidatedBook.getDescription(), consolidatedBook));

            final OrderBookOptions l2AggregatedOptions = new OrderBookOptionsBuilder()
                    .parent(l2CommonOptions)
                    .orderBookType(OrderBookType.AGGREGATED)
                    .build();

            final OrderBook<OrderBookQuote> aggregatedBook = OrderBookFactory.create(l2AggregatedOptions);
            resultBuilder.append(executed(symbol, startTime, endTime, stream, aggregatedBook::update, aggregatedBook.getDescription(), aggregatedBook));

            final OrderBookOptions l2SingledExchangeOptions = new OrderBookOptionsBuilder()
                    .parent(l2CommonOptions)
                    .orderBookType(OrderBookType.SINGLE_EXCHANGE)
                    .build();

            final OrderBook<OrderBookQuote> singleExchangeBook = OrderBookFactory.create(l2SingledExchangeOptions);
            resultBuilder.append(executed(symbol, startTime, endTime, stream, singleExchangeBook::update, singleExchangeBook.getDescription(), singleExchangeBook));

            LOGGER.info("\n %s").with(resultBuilder);

        } catch (Throwable t) {
            LOGGER.error().append("Failed to test").append(t).commit();
        } finally {
            db.close();
        }
    }

    private String executed(final String symbol,
                            final long startTime,
                            final long endTime,
                            final DXTickStream stream,
                            final Consumer<MarketMessageInfo> consumer,
                            final String className,
                            Object book) {
        double sum = 0;
        final int iteration = 20;
        for (int i = 0; i < iteration; i++) {
            sum += readStream(stream, symbol, startTime, endTime, consumer);
        }
        return String.format("Book Name = %s, Read AVG msg/s =  %d , Size bite(s) = %d, FootPrint = %s \n",
                className, (int) sum / iteration, GraphLayout.parseInstance(book).totalSize(), GraphLayout.parseInstance(book).toFootprint());
    }

}
