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

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.orderbook.core.api.EntryValidationCode;
import com.epam.deltix.orderbook.core.api.MarketSide;
import com.epam.deltix.timebase.messages.universal.InsertType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;

import java.util.ArrayList;
import java.util.Objects;

/**
 * This interface defines the behaviours of a L3 Market Side.
 *
 * @param <Quote> the quote type
 */
interface L3MarketSide<Quote> extends MarketSide<Quote> {
    /**
     * Creates and returns a new instance of {@link L3MarketSide} with the specified initial and maximum depth,
     * tailored for the given side of the market (BID or ASK).
     *
     * This factory method is a convenience for creating instances of {@code L3MarketSide} with either BID or ASK side
     * configurations without directly instantiating the concrete implementations. It encapsulates the creation logic
     * and ensures that the returned {@code L3MarketSide} is properly initialized with the provided parameters.
     *
     * <p>Example Usage:</p>
     * <pre>{@code
     * L3MarketSide<YourQuoteImplementation> bidSide = L3MarketSide.factory(10, 100, QuoteSide.BID);
     * }</pre>
     *
     * @param <Quote> the type parameter extending {@link MutableOrderBookQuote} which specifies the concrete type of the quote used in the order book
     * @param initialDepth the initial depth of the order book. Must be non-negative.
     * @param maxDepth the maximum depth the order book can grow to. Must be greater than or equal to {@code initialDepth}.
     * @param side the side of the order book ({@link QuoteSide#BID} for bids, {@link QuoteSide#ASK} for asks) to determine the market side behavior
     * @return a {@link L3MarketSide} instance configured with the specified initial and maximum depth, and market side
     * @throws IllegalStateException if the {@code side} is neither {@link QuoteSide#BID} nor {@link QuoteSide#ASK}
     * @throws NullPointerException if {@code side} is null
     */
    static <Quote extends MutableOrderBookQuote> L3MarketSide<Quote> factory(final int initialDepth,
                                                                             final int maxDepth,
                                                                             final QuoteSide side) {
        Objects.requireNonNull(side, "QuoteSide cannot be null.");
        switch (side) {
            case BID:
                return new AbstractL3MarketSide.BIDS<>(initialDepth, maxDepth);
            case ASK:
                return new AbstractL3MarketSide.ASKS<>(initialDepth, maxDepth);
            default:
                throw new IllegalStateException("Unexpected value: " + side);
        }
    }

    /**
     * Add a quote to the market side.
     *
     * @param insert the quote to be inserted
     * @return true if the quote was added successfully
     */
    boolean add(Quote insert);

    /**
     * Get max depth of order book.
     *
     * @return max depth
     */
    int getMaxDepth();

    /**
     * Remove a quote from the market side using its id.
     *
     * @param quoteId the id of the quote to be removed
     * @return the removed quote
     */
    Quote remove(CharSequence quoteId);

    /**
     * Remove a quote from the market side.
     *
     * @param quote the quote to be removed
     * @return the removed quote
     */
    Quote remove(Quote quote);

    /**
     * Get quote from the market using its id.
     *
     * @param quoteId the id of the quote to fetch
     * @return the fetched quote
     */
    Quote getQuote(CharSequence quoteId);

    /**
     * Checks if a quote with given id exists in the market side.
     *
     * @param quoteId the id of the quote
     * @return true if the quote exists
     */
    boolean hasQuote(CharSequence quoteId);

    /**
     * Checks if the current market side is full.
     *
     * @return true if the market side is full
     */
    boolean isFull();

    /**
     * Clear this market side by removing all quotes.
     */
    void clear();

    /**
     * Build this market side from a sorted list of quotes.
     *
     * @param quotes list of quotes to build the market side
     */
    void buildFromSorted(ArrayList<Quote> quotes);

    /**
     * Validates the specified quote parameters before they are inserted into the order book.
     *
     * This method checks whether the combination of parameters provided for a quote meets the criteria for insertion into the order book.
     * It is designed to ensure data integrity and consistency by validating the parameters against predefined rules.
     *
     * The validation covers various aspects such as the type of insertion, quote identifiers, price and size of the quote,
     * and the side of the market the quote is intended for. If any parameter does not comply with the expected criteria,
     * an appropriate error code is returned, indicating the reason for validation failure. Otherwise, if the quote is deemed valid,
     * a {@code null} value is returned, signifying that the quote can be safely inserted into the order book.
     *
     * <p>Usage example:</p>
     * <pre>{@code
     * EntryValidationCode validationCode = isInvalidInsert(InsertType.NEW, "quote123", 1000L, 10L, QuoteSide.BID);
     * if (validationCode != null) {
     *     // Handle validation failure as per validationCode
     * } else {
     *     // Proceed with quote insertion
     * }
     * }</pre>
     *
     * @param type The type of insertion operation, e.g., NEW, UPDATE, indicating the nature of the action to be performed on the order book.
     * @param quoteId A {@link CharSequence} representing the unique identifier of the quote to be validated.
     * @param price The price of the quote, which must be a positive long value representing the cost per unit.
     * @param size The size of the quote, indicating the number of units, which must be a positive long value.
     * @param side The {@link QuoteSide} indicating whether the quote is a bid or an ask in the market.
     * @return An {@link EntryValidationCode} enum instance indicating the type of validation error, or {@code null} if the quote is valid.
     */
    EntryValidationCode isInvalidInsert(InsertType type,
                                        CharSequence quoteId,
                                        @Decimal long price,
                                        @Decimal long size,
                                        QuoteSide side);

    /**
     * Validates the specified quote parameters before updating an existing quote in the order book.
     *
     * This method assesses whether the updated quote parameters conform to the validation criteria necessary for maintaining the integrity
     * and consistency of the order book data. It performs a thorough check on the updated values provided for the quote, including the quote
     * identifier, updated price and size values (annotated with {@code @Decimal} to emphasize their decimal nature in financial calculations),
     * and the market side (bid or ask) to which the quote belongs. If any updated parameter violates the validation rules, an appropriate error
     * code is returned to indicate the nature of the discrepancy. Conversely, if all parameters are deemed valid, the method returns {@code null},
     * signifying that the quote update operation can proceed.
     *
     * <p>Usage example:</p>
     * <pre>{@code
     * Quote someQuote = getQuoteFromOrderBook("quote123");
     * EntryValidationCode validationCode = isInvalidUpdate(someQuote, "quote123", 1000L, 50L, QuoteSide.ASK);
     * if (validationCode != null) {
     *     // Handle validation error based on the returned code
     * } else {
     *     // Proceed with quote update in the order book
     * }
     * }</pre>
     *
     * @param quote The Quote object representing the quote to be updated. This serves as a reference to the existing quote in the order book.
     * @param quoteId A {@link CharSequence} representing the unique identifier of the quote being validated for update. This is used to ensure the correct quote is targeted for update.
     * @param price The updated price for the quote, represented as a long value. The {@code @Decimal} annotation indicates its decimal nature, and it must be a positive value.
     * @param size The updated size of the quote, representing the number of units, again noted as a long with the {@code @Decimal} annotation, and must be a positive value.
     * @param side The {@link QuoteSide} indicating the market side of the quote, either BID or ASK, to which the update applies.
     * @return An {@link EntryValidationCode} indicating the validation result: an error code if the updated parameters are invalid, or {@code null} if the update is valid.
     */
    EntryValidationCode isInvalidUpdate(Quote quote,
                                        CharSequence quoteId,
                                        @Decimal long price,
                                        @Decimal long size,
                                        QuoteSide side);
}
