package com.epam.deltix.orderbook.core.options;

/**
 * Sample order book options.
 */
public class Presets {

    /**
     * Quote flow style order book.
     */
    public static final OrderBookOptions QF_COMMON = new OrderBookOptionsBuilder()
            .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
            .resetMode(ResetMode.WAITING_FOR_SNAPSHOT)
            .validationOptions(ValidationOptions.builder()
                    .validateQuoteInsert()
                    .skipInvalidQuoteUpdate()
                    .build())
            .disconnectMode(DisconnectMode.CLEAR_EXCHANGE)
            .periodicalSnapshotMode(PeriodicalSnapshotMode.ONLY_ONE)
            .build();
}
