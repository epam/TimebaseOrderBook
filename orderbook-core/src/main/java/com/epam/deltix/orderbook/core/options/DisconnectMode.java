package com.epam.deltix.orderbook.core.options;

/**
 * Enumeration of possible values for customization
 * of the modes of processing quotes in case of loss of connection with the exchange.
 *
 * @author Andrii_Ostapenko
 */
public enum DisconnectMode {
    /**
     * Clear stock exchange data after disconnect.
     */
    CLEAR_EXCHANGE,
    /**
     * Keeping outdated stock exchange data after disconnect.
     */
    KEEP_STALE_DATA

}
