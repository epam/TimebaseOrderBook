package com.epam.deltix.orderbook.core.api;

/**
 * Quote timestamp interface.
 * <p>
 * This interface is used for accessing quote timestamp information.
 * This functionality is optional and can be enabled by setting OrderBookOptionsBuilder.shouldStoreQuoteTimestamps(true)
 * <p>
 * This function requires additional memory allocation for each quote.
 *
 * @author Andrii_Ostapenko1
 * @see com.epam.deltix.orderbook.core.options.Defaults#SHOULD_STORE_QUOTE_TIMESTAMPS
 */
public interface OrderBookQuoteTimestamp {

    /**
     * Special constant that marks 'unknown' timestamp.
     */
    long TIMESTAMP_UNKNOWN = Long.MIN_VALUE;

    /**
     * Exchange Time is measured in milliseconds that passed since January 1, 1970 UTC
     * For inbound messages special constant {link TIMESTAMP_UNKNOWN} marks 'unknown' timestamp in which case OrderBook
     * stores message using current server time.
     * <p>
     * By default, Original Timestamp is not supported.
     * For enabling Original Timestamp support you need to set OrderBookOptionsBuilder.shouldStoreQuoteTimestamps(true)
     *
     * @return timestamp
     */
    default long getOriginalTimestamp() {
        return TIMESTAMP_UNKNOWN;
    }

    /**
     * Exchange Time is measured in milliseconds that passed since January 1, 1970 UTC
     * By default Original Timestamp is not supported.
     * For enabling Original Timestamp support you need to set OrderBookOptionsBuilder.shouldStoreQuoteTimestamps(true)
     *
     * @return true if Original Timestamp is not null
     */
    default boolean hasOriginalTimestamp() {
        return false;
    }

    /**
     * Time in this field is measured in milliseconds that passed since January 1, 1970 UTC.
     * For inbound messages, special constant {link TIMESTAMP_UNKNOWN} marks 'unknown' timestamp
     * in which case OrderBook stores message using current server time.
     * <p>
     * By default, TimeStamp is not supported.
     * For enabling Time support you need to set OrderBookOptionsBuilder.shouldStoreQuoteTimestamps(true)
     *
     * @return timestamp
     */
    default long getTimestamp() {
        return TIMESTAMP_UNKNOWN;
    }

    /**
     * Time in this field is measured in milliseconds that passed since January 1, 1970 UTC.
     * For inbound messages, special constant {link TIMESTAMP_UNKNOWN} marks 'unknown' timestamp
     * in which case OrderBook stores message using current server time.
     * <p>
     * By default, TimeStamp is not supported.
     * For enabling Time support, you need to set OrderBookOptionsBuilder.shouldStoreQuoteTimestamps(true)
     *
     * @return true if time not null
     */
    default boolean hasTimestamp() {
        return false;
    }
}
