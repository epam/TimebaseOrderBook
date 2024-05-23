package com.epam.deltix.orderbook.core.impl;


import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.orderbook.core.options.Defaults;
import com.epam.deltix.orderbook.core.options.OrderBookOptions;
import com.epam.deltix.timebase.messages.universal.DataModelType;

/**
 * @author Andrii_Ostapenko1
 */
public final class QuotePoolFactory {

    /**
     * Prevents instantiation
     */
    private QuotePoolFactory() {
    }

    //TODO add javadoc
    public static ObjectPool<? extends OrderBookQuote> create(final OrderBookOptions options,
                                                              final int initialSize) {
        final ObjectPool<? extends MutableOrderBookQuote> pool;
        // TODO: need to refactor
        final DataModelType quoteLevels = options.getQuoteLevels().get();
        if (options.shouldStoreQuoteTimestamps().orElse(Defaults.SHOULD_STORE_QUOTE_TIMESTAMPS)) {
            switch (quoteLevels) {
                case LEVEL_ONE:
                case LEVEL_TWO:
                    pool = new ObjectPool<>(initialSize, MutableOrderBookQuoteTimestampImpl::new, MutableOrderBookQuoteTimestampImpl::release);
                    break;
                case LEVEL_THREE:
                    pool = new ObjectPool<>(initialSize, MutableOrderBookQuoteL3TimestampImpl::new, MutableOrderBookQuoteL3TimestampImpl::release);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported book type: " + options.getBookType() +
                            " for quote levels: " + quoteLevels);
            }
        } else {
            switch (quoteLevels) {
                case LEVEL_ONE:
                case LEVEL_TWO:
                    pool = new ObjectPool<>(initialSize, MutableOrderBookQuoteImpl::new, MutableOrderBookQuoteImpl::release);
                    break;
                case LEVEL_THREE:
                    pool = new ObjectPool<>(initialSize, MutableOrderBookQuoteL3Impl::new, MutableOrderBookQuoteL3Impl::release);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported book type: " + options.getBookType() +
                            " for quote levels: " + quoteLevels);
            }

        }
        return pool;
    }
}
