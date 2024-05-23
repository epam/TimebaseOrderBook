package com.epam.deltix.orderbook.core.impl;


import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.universal.PackageType;

import static com.epam.deltix.timebase.messages.universal.PackageType.PERIODICAL_SNAPSHOT;
import static com.epam.deltix.timebase.messages.universal.PackageType.VENDOR_SNAPSHOT;

class EventHandlerImpl implements EventHandler {

    /**
     * This parameter using for handle periodical snapshot entry and depends on {@link PeriodicalSnapshotMode}.
     */
    private boolean isPeriodicalSnapshotAllowed;

    /**
     * This parameter using for handle incremental update entry and depends on {@link UpdateMode} and {@link ResetMode}.
     */
    private boolean isWaitingForSnapshot;

    //Parameters
    private final UpdateMode updateMode;
    private final ResetMode resetMode;
    private final PeriodicalSnapshotMode periodicalSnapshotMode;
    private final DisconnectMode disconnectMode;

    EventHandlerImpl(final OrderBookOptions options) {
        this.disconnectMode = options.getDisconnectMode().orElse(Defaults.DISCONNECT_MODE);
        this.updateMode = options.getUpdateMode().orElse(Defaults.UPDATE_MODE);
        this.resetMode = options.getResetMode().orElse(Defaults.RESET_MODE);
        this.periodicalSnapshotMode = options.getPeriodicalSnapshotMode().orElse(Defaults.PERIODICAL_SNAPSHOT_MODE);

        this.isPeriodicalSnapshotAllowed = periodicalSnapshotMode != PeriodicalSnapshotMode.SKIP_ALL;
        this.isWaitingForSnapshot = (updateMode == UpdateMode.WAITING_FOR_SNAPSHOT);
    }

    @Override
    public boolean isSnapshotAllowed(final PackageType type) {
        if (type == null) {
            return false;
        }

        if (type == VENDOR_SNAPSHOT) {
            return true;
        }

        return isPeriodicalSnapshotAllowed && type == PERIODICAL_SNAPSHOT;
    }

    @Override
    public boolean isWaitingForSnapshot() {
        return isWaitingForSnapshot;
    }

    @Override
    public void onDisconnect() {
        isWaitingForSnapshot = (updateMode == UpdateMode.WAITING_FOR_SNAPSHOT); //TODO: review: switch to (disconnectMode == CLEAR_EXCHANGE) ?
        isPeriodicalSnapshotAllowed = periodicalSnapshotMode != PeriodicalSnapshotMode.SKIP_ALL;
    }

    @Override
    public void onReset() {
        isWaitingForSnapshot = (resetMode == ResetMode.WAITING_FOR_SNAPSHOT);
        isPeriodicalSnapshotAllowed = periodicalSnapshotMode != PeriodicalSnapshotMode.SKIP_ALL;
    }

    @Override
    public void onSnapshot() {
        isWaitingForSnapshot = false;
        isPeriodicalSnapshotAllowed = periodicalSnapshotMode == PeriodicalSnapshotMode.PROCESS_ALL;
    }

    @Override
    public void onBroken() {
        isWaitingForSnapshot = true;
        isPeriodicalSnapshotAllowed = periodicalSnapshotMode != PeriodicalSnapshotMode.SKIP_ALL;
    }
}
