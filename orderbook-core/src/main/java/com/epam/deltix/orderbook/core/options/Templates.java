package com.epam.deltix.orderbook.core.options;

/**
 * @author Andrii_Ostapenko1
 */
public final class Templates {

    /**
     * Utility class.
     */
    private Templates() {
        throw new IllegalStateException("No instances!");
    }

    public static final OrderBookOptions QUOTE_FLOW = new OrderBookOptionsBuilder()
            .resetMode(ResetMode.NON_WAITING_FOR_SNAPSHOT)
            .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
            .disconnectMode(DisconnectMode.CLEAR_EXCHANGE)
            .build();
}
