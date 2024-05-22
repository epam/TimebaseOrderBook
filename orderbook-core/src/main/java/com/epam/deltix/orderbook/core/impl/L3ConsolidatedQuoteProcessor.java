package com.epam.deltix.orderbook.core.impl;


import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.api.EntryValidationCode;
import com.epam.deltix.orderbook.core.impl.collections.rbt.RBTree;
import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.annotations.Alphanumeric;
import com.epam.deltix.util.collections.generated.ObjectList;

import java.util.*;

import static com.epam.deltix.dfp.Decimal64Utils.ZERO;
import static com.epam.deltix.timebase.messages.universal.QuoteSide.ASK;
import static com.epam.deltix.timebase.messages.universal.QuoteSide.BID;

/**
 * @author Andrii_Ostapenko1
 */
class L3ConsolidatedQuoteProcessor<Quote extends MutableOrderBookQuote> implements L3Processor<Quote> {

    protected final L3MarketSide<Quote> bids;
    protected final L3MarketSide<Quote> asks;

    protected final ObjectPool<Quote> pool;

    protected final MutableExchangeList<MutableExchange<Quote, L3Processor<Quote>>> exchanges;

    //Parameters
    protected final DisconnectMode disconnectMode;
    protected final ValidationOptions validationOptions;
    private final OrderBookOptions options;

    L3ConsolidatedQuoteProcessor(final OrderBookOptions options,
                                 final ObjectPool<Quote> pool) {
        this.options = options;
        this.validationOptions = options.getInvalidQuoteMode().orElse(Defaults.VALIDATION_OPTIONS);
        this.disconnectMode = options.getDisconnectMode().orElse(Defaults.DISCONNECT_MODE);

        final int maxDepth = options.getMaxDepth().orElse(Defaults.MAX_DEPTH);
        final int initialDepth = options.getInitialDepth().orElse(Math.min(Defaults.INITIAL_DEPTH, maxDepth));
        final int numberOfExchanges = options.getInitialExchangesPoolSize().orElse(Defaults.INITIAL_EXCHANGES_POOL_SIZE);
        this.pool = pool;
        this.exchanges = new MutableExchangeListImpl<>(numberOfExchanges);
        this.asks = new ConsolidatedL3MarketSide.ASKS<>(numberOfExchanges * initialDepth, Defaults.MAX_DEPTH);
        this.bids = new ConsolidatedL3MarketSide.BIDS<>(numberOfExchanges * initialDepth, Defaults.MAX_DEPTH);
    }

    @Override
    public boolean isWaitingForSnapshot() {
        if (exchanges.isEmpty()) {
            return true; // No data from exchanges, so we are in "waiting" state
        }

        for (final MutableExchange exchange : exchanges) {
            if (exchange.isWaitingForSnapshot()) {
                return true; // At least one of source exchanges awaits snapshot
            }
        }
        return false;
    }

    @Override
    public boolean isSnapshotAllowed(final PackageHeaderInfo msg) {
        throw new UnsupportedOperationException("Unsupported for multiexchange processor!");
    }

    @Override
    public MutableExchangeList<MutableExchange<Quote, L3Processor<Quote>>> getExchanges() {
        return exchanges;
    }

    @Override
    public L3MarketSide<Quote> getMarketSide(final QuoteSide side) {
        return side == BID ? bids : asks;
    }

    @Override
    public boolean isEmpty() {
        return asks.isEmpty() && bids.isEmpty();
    }

    @Override
    public boolean processSecurityFeedStatus(final SecurityFeedStatusMessage msg) {
        if (msg.getStatus() == FeedStatus.NOT_AVAILABLE) {
            if (disconnectMode == DisconnectMode.CLEAR_EXCHANGE) {
                @Alphanumeric final long exchangeId = msg.getExchangeId();
                final Option<MutableExchange<Quote, L3Processor<Quote>>> holder = getOrCreateExchange(exchangeId);

                if (!holder.hasValue()) {
                    return false;
                }
                final L3Processor<Quote> exchange = holder.get().getProcessor();

                subtractExchange(exchange);
                return exchange.processSecurityFeedStatus(msg);
            }
        }
        return false;
    }

    @Override
    public boolean processBookResetEntry(final PackageHeaderInfo pck, final BookResetEntryInfo msg) {
        @Alphanumeric final long exchangeId = msg.getExchangeId();
        final Option<MutableExchange<Quote, L3Processor<Quote>>> holder = getOrCreateExchange(exchangeId);

        if (!holder.hasValue()) {
            return false;
        }
        final L3Processor<Quote> exchange = holder.get().getProcessor();

        subtractExchange(exchange);
        return exchange.processBookResetEntry(pck, msg);
    }

    @Override
    public boolean processL3Snapshot(final PackageHeaderInfo msg) {
        final ObjectList<BaseEntryInfo> entries = msg.getEntries();

        // we assume that all entries in the message are from the same exchange
        @Alphanumeric final long exchangeId = entries.get(0).getExchangeId();
        final Option<MutableExchange<Quote, L3Processor<Quote>>> holder = getOrCreateExchange(exchangeId);

        if (!holder.hasValue()) {
            return false;
        }

        final L3Processor<Quote> exchange = holder.get().getProcessor();
        if (exchange.isSnapshotAllowed(msg)) {
            subtractExchange(exchange);
            if (exchange.processL3Snapshot(msg)) {
                addExchange(exchange);
                return true;
            }
        }
        return false;
    }

    @Override
    public Quote processL3EntryNew(final PackageHeaderInfo pck, final L3EntryNewInfo msg) {
        final QuoteSide side = msg.getSide();
        @Alphanumeric final long exchangeId = msg.getExchangeId();

        final Option<MutableExchange<Quote, L3Processor<Quote>>> holder = getOrCreateExchange(exchangeId);
        if (!holder.hasValue() || holder.get().getProcessor().isWaitingForSnapshot()) {
            return null;
        }

        final L3Processor<Quote> exchange = holder.get().getProcessor();
        final L3MarketSide<Quote> marketSide = exchange.getMarketSide(side);
        final EntryValidationCode errorCode = marketSide.isInvalidInsert(msg.getInsertType(), msg.getQuoteId(), msg.getPrice(), msg.getSize(), side);
        if (errorCode != null) {
            if (validationOptions.isQuoteInsert()) {
                subtractExchange(exchange);
            }
            exchange.processL3EntryNew(pck, msg);
            return null;
        }

        final Quote quote;
        final L3MarketSide<Quote> consolidatedMarketSide = getMarketSide(side);
        if (marketSide.isFull()) { // CAREFUL! In this case we can't guarantee uniqueness of quoteIds
            final Quote worstQuote = marketSide.getWorstQuote();
            if (side == ASK && Decimal64Utils.isGreater(worstQuote.getPrice(), msg.getPrice()) ||
                    side == BID && Decimal64Utils.isGreater(msg.getPrice(), worstQuote.getPrice())) {
                quote = marketSide.remove(worstQuote.getQuoteId());
                consolidatedMarketSide.remove(quote);
            } else {
                return null;
            }
        } else {
            quote = pool.borrow();
        }

        quote.copyFrom(pck, msg);
        if (!marketSide.add(quote)) {
            pool.release(quote);
            if (validationOptions.isQuoteInsert()) {
                subtractExchange(exchange);
            }
            exchange.processL3EntryNew(pck, msg);
            return null;
        }
        consolidatedMarketSide.add(quote);
        return quote;
    }

    public boolean handleReplace(final L3Processor<Quote> exchange,
                                 final PackageHeaderInfo pck,
                                 final L3EntryUpdateInfo msg) {
        final QuoteSide side = msg.getSide();
        final CharSequence quoteId = msg.getQuoteId();
        final L3MarketSide<Quote> newSide = exchange.getMarketSide(side);
        final L3MarketSide<Quote> consolidatedNewSide = getMarketSide(side);

        final EntryValidationCode errorCode = newSide.isInvalidInsert(InsertType.ADD_BACK, msg.getQuoteId(), msg.getPrice(), msg.getSize(), side);
        if (errorCode != null) {
            subtractExchange(exchange);
            exchange.processL3EntryUpdate(pck, msg);
        }

        final Quote quote = newSide.remove(quoteId);
        if (quote != null) { // replace didn't change side
            consolidatedNewSide.remove(quote);
            quote.copyFrom(pck, msg);
            newSide.add(quote);
            consolidatedNewSide.add(quote);
            return true;
        }

        final L3MarketSide<Quote> prevSide = exchange.getMarketSide(side == ASK ? BID : ASK);
        final L3MarketSide<Quote> consolidatedPrevSide = getMarketSide(side == ASK ? BID : ASK);
        final Quote removed = prevSide.remove(quoteId);
        if (removed != null) { // replace changed side
            consolidatedPrevSide.remove(removed);
            Quote newQuote = removed;
            if (newSide.isFull()) {
                pool.release(removed);
                final Quote worstQuote = newSide.getWorstQuote();
                if (side == ASK && Decimal64Utils.isGreater(worstQuote.getPrice(), msg.getPrice()) ||
                        side == BID && Decimal64Utils.isGreater(msg.getPrice(), worstQuote.getPrice())) {
                    newQuote = newSide.remove(worstQuote.getQuoteId());
                    consolidatedNewSide.remove(newQuote);
                } else {
                    return true;
                }
            }
            newQuote.copyFrom(pck, msg);
            newSide.add(newQuote);
            consolidatedNewSide.add(newQuote);
            return true;
        }

        if (validationOptions.isQuoteUpdate()) {
            subtractExchange(exchange);
        }
        return exchange.processL3EntryUpdate(pck, msg);
    }

    public boolean handleCancel(final L3Processor<Quote> exchange,
                                final PackageHeaderInfo pck,
                                final L3EntryUpdateInfo msg) {
        final CharSequence quoteId = msg.getQuoteId();
        final QuoteSide side = msg.getSide() == ASK ? ASK : BID;

        Quote removed = exchange.getMarketSide(side).remove(quoteId);
        if (removed == null) {
            // setting it as ASK would suffice when side is set correctly or not set at all (null)
            removed = exchange.getMarketSide(side == ASK ? BID : ASK).remove(quoteId);
            if (removed != null) {
                getMarketSide(side == ASK ? BID : ASK).remove(removed);
            }
        } else {
            getMarketSide(side).remove(removed);
        }

        if (removed == null) {
            if (validationOptions.isQuoteUpdate()) {
                subtractExchange(exchange);
            }
            return exchange.processL3EntryUpdate(pck, msg);
        }
        pool.release(removed);
        return true;
    }

    public boolean handleModify(final L3Processor<Quote> exchange, final PackageHeaderInfo pck, final L3EntryUpdateInfo msg) {
        final QuoteSide side = msg.getSide(); // probably we should validate that side != null immediately?
        final CharSequence quoteId = msg.getQuoteId();
        final L3MarketSide<Quote> marketSide = exchange.getMarketSide(side);
        final Quote quote = marketSide.getQuote(quoteId);

        final EntryValidationCode errorCode = marketSide.isInvalidUpdate(quote, msg.getQuoteId(), msg.getPrice(), msg.getSize(), side);
        if (errorCode != null) {
            if (validationOptions.isQuoteUpdate()) {
                subtractExchange(exchange);
            }
            return exchange.processL3EntryUpdate(pck, msg);
        }

        quote.copyFrom(pck, msg);
        return true;
    }

    @Override
    public boolean processL3EntryUpdate(final PackageHeaderInfo pck,
                                        final L3EntryUpdateInfo msg) {
        @Alphanumeric final long exchangeId = msg.getExchangeId();

        final Option<MutableExchange<Quote, L3Processor<Quote>>> holder = getExchanges().getById(exchangeId);
        if (!holder.hasValue() || holder.get().getProcessor().isWaitingForSnapshot()) {
            return false;
        }

        final L3Processor<Quote> exchange = holder.get().getProcessor();
        final QuoteUpdateAction action = msg.getAction();
        if (action == QuoteUpdateAction.CANCEL) {
            return handleCancel(exchange, pck, msg);
        }
        if (action == QuoteUpdateAction.REPLACE) {
            return handleReplace(exchange, pck, msg);
        }
        if (action == QuoteUpdateAction.MODIFY) {
            return handleModify(exchange, pck, msg);
        }
        if (validationOptions.isQuoteUpdate()) {
            subtractExchange(exchange);
        }
        return exchange.processL3EntryUpdate(pck, msg);
    }

    private void addExchange(final L3Processor<Quote> exchange) {
        {
            final L3MarketSide<Quote> srcMarketSide = exchange.getMarketSide(ASK);
            final L3MarketSide<Quote> dstMarketSide = getMarketSide(ASK);
            for (final Quote quote : srcMarketSide) {
                dstMarketSide.add(quote);
            }
        }

        {
            final L3MarketSide<Quote> srcMarketSide = exchange.getMarketSide(BID);
            final L3MarketSide<Quote> dstMarketSide = getMarketSide(BID);
            for (final Quote quote : srcMarketSide) {
                dstMarketSide.add(quote);
            }
        }
    }

    public void subtractExchange(final L3Processor<Quote> exchange) {
        {
            final L3MarketSide<Quote> srcMarketSide = exchange.getMarketSide(ASK);
            final L3MarketSide<Quote> dstMarketSide = getMarketSide(ASK);
            for (final Quote quote : srcMarketSide) {
                dstMarketSide.remove(quote);
            }
        }

        {
            final L3MarketSide<Quote> srcMarketSide = exchange.getMarketSide(BID);
            final L3MarketSide<Quote> dstMarketSide = getMarketSide(BID);
            for (final Quote quote : srcMarketSide) {
                dstMarketSide.remove(quote);
            }
        }
    }

    /**
     * Get stock exchange holder by id(create new if it does not exist).
     *
     * @param exchangeId - id of exchange.
     * @return exchange book by id.
     */
    private Option<MutableExchange<Quote, L3Processor<Quote>>> getOrCreateExchange(@Alphanumeric final long exchangeId) {
        if (!AlphanumericUtils.isValidAlphanumeric(exchangeId) || TypeConstants.EXCHANGE_NULL == exchangeId) {
            return Option.empty();
        }
        final MutableExchangeList<MutableExchange<Quote, L3Processor<Quote>>> exchanges = this.getExchanges();
        Option<MutableExchange<Quote, L3Processor<Quote>>> holder = exchanges.getById(exchangeId);
        if (!holder.hasValue()) {
            final L3Processor<Quote> processor = new L3SingleExchangeQuoteProcessor<>(options, pool);
            exchanges.add(new MutableExchangeImpl<>(exchangeId, processor));
            holder = exchanges.getById(exchangeId);
        }
        return holder;
    }

    @Override
    public String getDescription() {
        return "L3/Consolidation of multiple exchanges";
    }

    @Override
    public void clear() {
        asks.clear();
        bids.clear();
        for (final MutableExchange<Quote, L3Processor<Quote>> exchange : this.getExchanges()) {
            exchange.getProcessor().clear();
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", L3ConsolidatedQuoteProcessor.class.getSimpleName() + "[", "]")
                .add("exchanges=" + exchanges.size())
                .add("bids=" + bids.depth())
                .add("asks=" + asks.depth())
                .toString();
    }

    abstract static class ConsolidatedL3MarketSide<Quote extends MutableOrderBookQuote> implements L3MarketSide<Quote> {
        protected final RBTree<Quote, Quote> data;
        private final ReusableIterator<Quote> itr;
        private final int maxDepth;
        private long virtualClock;

        ConsolidatedL3MarketSide(final int initialCapacity, final int maxDepth) {
            this.maxDepth = maxDepth;
            this.data = new RBTree<>(initialCapacity, new QuoteComparator());
            this.itr = new ReusableIterator<>();
            virtualClock = Long.MIN_VALUE;
        }

        @Override
        public int getMaxDepth() {
            return maxDepth;
        }

        @Override
        public int depth() {
            return data.size();
        }

        @Override
        public long getTotalQuantity() {
            @Decimal long result = ZERO;
            for (final Quote quote : this) {
                result = Decimal64Utils.add(result, quote.getSize());
            }
            return result;
        }

        /**
         * Clears the market side in linear time
         */
        @Override
        public void clear() {
            data.clear();
        }

        @Override
        public boolean isEmpty() {
            return data.isEmpty();
        }

        @Override
        public Quote getQuote(final int level) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Quote getQuote(final CharSequence quoteId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(final Quote insert) {
            insert.setSequenceNumber(virtualClock++);
            final Quote res = data.put(insert, insert);
            assert res == null;
            return true;
        }

        @Override
        public Quote remove(final Quote delete) {
            final Quote res = data.remove(delete);
            assert res != null;
            return res;
        }

        @Override
        public Quote remove(final CharSequence quoteId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isFull() {
            return depth() == maxDepth;
        }

        @Override
        public Quote getBestQuote() {
            if (isEmpty()) {
                return null;
            }
            return data.firstKey();
        }

        @Override
        public boolean hasLevel(final int level) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasQuote(final CharSequence quoteId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Quote getWorstQuote() {
            if (isEmpty()) {
                return null;
            }
            return data.lastKey();
        }

        /**
         * @return error code, or null if everything is valid
         */
        @Override
        public EntryValidationCode isInvalidInsert(final InsertType type,
                                                   final CharSequence quoteId,
                                                   final @Decimal long price,
                                                   final @Decimal long size,
                                                   final QuoteSide side) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EntryValidationCode isInvalidUpdate(final Quote quote,
                                                   final CharSequence quoteId,
                                                   final @Decimal long price,
                                                   final @Decimal long size,
                                                   final QuoteSide side) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void buildFromSorted(final ArrayList<Quote> quotes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            for (final Quote quote : this) {
                builder.append(quote).append("\n");
            }
            return builder.toString();
        }

        @Override
        public Iterator<Quote> iterator(final int fromLevel, final int toLevel) {
            if (fromLevel != 0) {
                throw new UnsupportedOperationException();
            }
            itr.iterateBy(data);
            return itr;
        }

        /**
         * An adapter to safely externalize the value iterator.
         */
        static final class ReusableIterator<Quote> implements Iterator<Quote> {

            private Iterator<Map.Entry<Quote, Quote>> iterator;

            private void iterateBy(final RBTree<Quote, Quote> tm) {
                Objects.requireNonNull(tm);
                iterator = tm.iterator();
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Quote next() {
                return iterator.next().getValue();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Read only iterator");
            }
        }

        static class ASKS<Quote extends MutableOrderBookQuote> extends ConsolidatedL3MarketSide<Quote> {

            ASKS(final int initialDepth, final int maxDepth) {
                super(initialDepth, maxDepth);
            }

            @Override
            public QuoteSide getSide() {
                return ASK;
            }

        }

        static class BIDS<Quote extends MutableOrderBookQuote> extends ConsolidatedL3MarketSide<Quote> {

            BIDS(final int initialDepth, final int maxDepth) {
                super(initialDepth, maxDepth);
            }

            @Override
            public QuoteSide getSide() {
                return BID;
            }

        }

        class QuoteComparator implements Comparator<Quote> {

            @Override
            public int compare(final Quote o1, final Quote o2) {
                final int priceComp = Decimal64Utils.compareTo(o1.getPrice(), o2.getPrice());
                if (priceComp == 0) {
                    return Long.compare(o1.getSequenceNumber(), o2.getSequenceNumber());
                }
                if (getSide() == ASK) {
                    return priceComp;
                } else {
                    return -priceComp;
                }
            }
        }
    }
}
