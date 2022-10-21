<img align="right" src="assets/img/timebase-logo.svg">

# Garbage Free OrderBook

[![GitHub](https://img.shields.io/badge/License-Apache--2.0-blue)](https://github.com/epam/TimebaseOrderBook/blob/main/LICENSE)

![build](https://github.com/epam/TimebaseOrderBook/workflows/Continuous%20Integration/badge.svg?branch=main)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/epam/TimebaseOrderBook.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/epam/TimebaseOrderBook/alerts/)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/epam/TimebaseOrderBook.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/epam/TimebaseOrderBook/context:java)

# Overview

This library provides lightweight Order Book component.

Requirements:

* Input data: ***Level 1***, ***Level 2*** market data in Universal Market Data format
* You need to decide if you need ***aggregated / consolidated / single-exchange*** order book before you start using it
* No need to keep "before/after" state of each market message update (only "after")
* No memory allocatons in main processing cycle
* Single threaded use only

# Getting started

***Step 1:*** Setting up the dependency.

The first step is to include `orderbook` into your project, for example, as a Gradle compile dependency:

```
dependencies {
    implementation  com.epam.deltix:orderbook-core:x.x.x
}
```

(Please replace ***x*** with the latest version numbers: [Maven Central](https://search.maven.org/search?q=g:com.epam.deltix) )

***Step 2:*** Create order book

  ```java
final OrderBook<OrderBookQuote> orderBook = OrderBookFactory.create();
  ```

***Step 3:*** Feed order book with market data

```java
void onMarketData(final MarketMessage message) {
    orderBook.update(message);
}
```

***Step 4:*** Access order book state

 - Use for-each loop (procedural style)

```java
for (OrderBookQuote quote : orderBook.getMarketSide(QuoteSide.ASK)) {
    System.out.println(quote);
}
```
   - You can use the streaming API if you don't care about memory allocation! :muscle:

```java
orderBook.getMarketSide(QuoteSide.ASK).stream()    #1) Stream for quotes in order book
    .filter(quote -> Decimal64Utils.isGreater(quote.getPrice(), Decimal64Utils.fromInt(15)))
    .forEach(System.out::println);

orderBook.getExchangeList().stream()               #2) Stream for quotes by exchanges
    .flatMap(exchange -> exchange.getMarketSide(QuoteSide.ASK).stream())
    .filter(quote -> Decimal64Utils.isGreater(quote.getPrice(), Decimal64Utils.fromInt(15)))
    .forEach(System.out::println);
```


# 	:wrench: Configuration Parameters

## :rocket: Initialization

You can use the `OrderBookOptionsBuilder` class like so:
```java
final OrderBookOptions commonOpt = new OrderBookOptionsBuilder()
        .quoteLevels(DataModelType.LEVEL_TWO)
        .initialMaxDepth(marketDepth)
        .initialExchangesPoolSize(exchangeIds.length)
        .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
        .build();

final OrderBookOptions opt = new OrderBookOptionsBuilder()
        .parent(commonOpt)
        .symbol("BTC/USD")
        .orderBookType(OrderBookType.AGGREGATED)
        .build();

final OrderBook<OrderBookQuote> orderBook = OrderBookFactory.create(opt);
```
or directly instantiate a OrderBook with default parameters like so:

```java
final OrderBook<OrderBookQuote> orderBook = OrderBookFactory.create();
```

## Parameter Details

- ***parent*** - Override the defaults from the given option <br>
  You may use this only once.
  <br>Since: ***1.0.11***
  <br>Type: OrderBookOptions


- ***symbol*** - Stock symbol<br>
  This stock symbol is used to check all input packets before processing.<br>
  If you are sure that your market data contains data for only one stock symbol, you may not set this option.
  <br>Since: ***1.0.11***
  <br>Type: String


- ***orderBookType*** - Order book type to use
  <br>Since: ***1.0.11***
  <br>Type: OrderBookType
  <br>Default Value is: ***SINGLE_EXCHANGE***
  <br>The following types are supported:
  * *SINGLE_EXCHANGE* - order book from single exchange 
  * *CONSOLIDATED*   - consolidated view on the market from multiple exchanges, you can see individual exchange sizes 
  * *AGGREGATED*     - aggregated view of multiple exchanges, you can see combined size of each price level 
   

 - ***updateMode*** -  What do we do with incremental update if we have empty order book?
   <br>Since: ***1.0.11***
   <br>Type: UpdateMode
   <br>Default Value is: ***WAITING_FOR_SNAPSHOT***
   <br>The following types are supported:
   * *WAITING_FOR_SNAPSHOT*       - waiting snapshot before processing incremental update
   * *NON_WAITING_FOR_SNAPSHOT*   - process incremental update without waiting for the snapshot


- ***gapMode*** - What do we do if we have a gap between the last existing level and the current inserted level (empty levels in between)? 
  <br>Since: ***1.0.11***
  <br>Type: GapMode
  <br>Default Value is: ***SKIP***
  <br>The following types are supported:
    * *SKIP* - let's skip quote
    * *SKIP_AND_DROP*   - let's skip quote and drop all quote for stock exchange
    * *FILL_GAP*   - let's fill these empty levels with values from the current event. <br>
      The insertion level cannot be greater than the value of the parameter ***maxDepth*** <br>
     otherwise insertion wiil be skip.

 
 - ***quoteLevels*** - Quote levels to use
   <br>Since: ***1.0.11***
   <br>Type: DataModelType 
   <br>Default Value is: ***LEVEL_ONE***
   <br>The following types are supported: 
    * *LEVEL_ONE*                - level one (best bid and best offer) 
    * *LEVEL_TWO*                - level two. Market by level. More details than LEVEL_ONE


 - :1234: ***initialDepth*** - How large initial depth of market should be?
   <br>Since: ***1.0.11***
   <br>Type: int 
   <br>Default Value is: ***1***


 - :1234: ***maxDepth*** - How large maximum (limit) depth of market should be? <br>
   Using this parameter if you want processing not all depth of market. <br>
   But is can make orderbook not valid (with gaps). See parameter ***gapMode***
   <br>Since: ***1.0.11***
   <br>Type: int
   <br>Default Value is: ***32767***


 - :1234: ***initialExchangesPoolSize*** - How large initial pool size for stock exchanges should be?<br>
   Supported for AGGREGATED and CONSOLIDATED order book type. 
   <br>Since: ***1.0.11***
   <br>Type: int 
   <br>Default Value is: ***1***
 

# Samples
## Iteration Cookies (Callback)

In many cases it is helpful to pass some kind of cookie or state object when iterating order book:

```java
orderBook.getMarketSide(QuoteSide.ASK).forEach(this::quoteViewAccumulatorAction, new QuoteCookie());
```

Small example that shows some useful accumulator:

```java
private static final class PriceAccumulator { 
    @Decimal
    private long price;

    public void apply(@Decimal long add) {
        price = Decimal64Utils.add(price, add);
    }

    public long getPrice() {
        return price;
    }
}

...
orderBook.getMarketSide(QuoteSide.ASK).forEach(this::quoteViewAccumulatorAction, priceAccumulator);
orderBook.getMarketSide(QuoteSide.BID).forEach(this::quoteViewAccumulatorAction, priceAccumulator);
...

boolean quoteViewAccumulatorAction(final OrderBookQuoteView orderBookQuote,
                                   final PriceAccumulator accumulator){
    accumulator.apply(orderBookQuote.getPrice());
    return priceLevel< 10; // do not go deeper than 10 levels
}
```
Samples can be found in the  [samples](./orderbook-sample/src/main/java/deltix/common/orderbook/) folder.

# Performance Testing

### Preparation

Measured on moderately sized developer's laptop. For run test see [***OrderBookIT***](./orderbook-it/src/main/java/deltix/common/orderbook/OrderBookIT.java#L101)

- ***Hardware:***

      CPU: Intel(R) Core(TM) i7-10610U CPU @ 1.80GHz   2.30 GHz
      Memory: 32.0 GB
      Disk: SSD 512

- ***Market data:***

      Quote levels: L1/L2
      Types of packages: VENDOR_SNAPSHOT/PERIODICAL_SNAPSHOT/INCREMENTAL_UPDATE
      Number of exchanges: 1
      Price levels: 500
      Number of messages: 4 27 042

### Test Result

*Table - ***L1 Quote Level****

Order Book Implementation  | Messages Processed  (msg/s)| Allocation Memory byte(s)
| :-------- | :------| :------|
Black hole                 |  ***1 977 046***   |  -
SingleExchangeOrderBook    |  ***1 567 812***    |  ***496***
ConsolidatedOrderBook      |  TODO      |  TODO
AggregatedOrderBook        |  TODO      |  TODO

*Table - ***L2 Quote Level****

Order Book Implementation  | Messages Processed  (msg/s)| Allocation Memory byte(s)
| :-------- | :------| :------|
Black hole                 |  ***1 977 046***   |  -
SingleExchangeOrderBook    |  ***1 110 222***    |  ***62 272***
ConsolidatedOrderBook      |  ***890 701***      |  ***67 024***
AggregatedOrderBook        |  ***890 062***      |  ***125 024***

# License
Copyright (C) 2022 EPAM

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
