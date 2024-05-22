package com.epam.deltix.orderbook.core.api;

/**
 * Represents validation error codes that can be encountered during order book updates.
 * Each constant corresponds to a specific scenario where an update to the order book fails
 * validation checks.
 */
public enum EntryValidationCode {
    /**
     * The quote ID, which uniquely identifies an order, is missing.
     */
    MISSING_QUOTE_ID,

    /**
     * The quote ID submitted already exists in the order book, indicating a duplicated order.
     */
    DUPLICATE_QUOTE_ID,

    /**
     * The price field of the order is missing, which is required for order placement.
     */
    MISSING_PRICE,

    /**
     * The submitted order size is either not well-formed or outside the allowed range.
     */
    BAD_SIZE,

    /**
     * The quote ID referenced does not match any existing order in the order book.
     */
    UNKNOWN_QUOTE_ID,

    /**
     * An attempt to modify an existing order with a new price was made,
     * which may not be supported in certain order book implementations.
     */
    MODIFY_CHANGE_PRICE,

    /**
     * An attempt to increase the size of an existing order through modification was made.
     * This may be an invalid operation depending on exchange rules.
     */
    MODIFY_INCREASE_SIZE,

    /**
     * The exchange ID, used to identify the marketplace where the order should be placed, is missing.
     */
    MISSING_EXCHANGE_ID,

    /**
     * There's a discrepancy between the exchange ID stated and the one expected by the order book.
     */
    EXCHANGE_ID_MISMATCH,

    /**
     * The action specified for updating the order book is not recognized or allowed.
     */
    UNSUPPORTED_UPDATE_ACTION,

    /**
     * The order does not specify whether it is a buy or sell order.
     */
    UNSPECIFIED_SIDE,

    /**
     * The type of order insert operation specified is not supported by the order book.
     */
    UNSUPPORTED_INSERT_TYPE
}
