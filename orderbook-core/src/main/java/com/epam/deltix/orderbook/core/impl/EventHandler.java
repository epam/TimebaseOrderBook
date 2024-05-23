package com.epam.deltix.orderbook.core.impl;


import com.epam.deltix.timebase.messages.universal.PackageType;

/**
 * Callback interface to be implemented for processing events as they become available in the {@link deltix.orderbook.core.api.OrderBook}
 */
interface EventHandler {
    /**
     * Check if snapshot type is available for processing.
     *
     * @param type snapshot type
     * @return true if snapshot is available for processing
     */
    boolean isSnapshotAllowed(PackageType type);

    /**
     * @return true if we can't process incremental update messages but waiting for next snapshot message
     */
    boolean isWaitingForSnapshot();

    /**
     * Call this method when book lost connection with feed.
     */
    void onDisconnect();

    /**
     * Call this method when book received reset entry.
     */
    void onReset();

    /**
     * Call this method when book received snapshot.
     */
    void onSnapshot();


    /**
     * Call this method when book received invalid date.
     */
    void onBroken();
}
