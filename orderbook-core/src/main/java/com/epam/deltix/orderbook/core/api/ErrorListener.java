package com.epam.deltix.orderbook.core.api;


import com.epam.deltix.timebase.messages.MessageInfo;

/**
 * User-defined error handler
 */
public interface ErrorListener {
    /**
     * Called when input market message contains something invalid.
     *
     * @param message   message containing invalid market-related messages
     * @param errorCode error code that describes what is wrong
     */
    void onError(MessageInfo message, EntryValidationCode errorCode);
}
