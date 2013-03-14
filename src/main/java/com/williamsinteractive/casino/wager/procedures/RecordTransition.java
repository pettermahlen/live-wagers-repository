package com.williamsinteractive.casino.wager.procedures;

import com.google.common.collect.ImmutableMap;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

import java.util.Map;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */

public class RecordTransition extends VoltProcedure {
    private final SQLStmt INSERT_WAGER_ROUND = new SQLStmt(
        "INSERT INTO wager_round (wager_round_id, game_id, exchange_rate_id)" +
            " VALUES (?, ?, ?)"
    );
    private final SQLStmt INSERT = new SQLStmt(
        "INSERT INTO wager_state (wager_round_id, wager_id, state, amount, created)" +
            " VALUES (?, ?, ?, ?, ?)"
    );
    private final SQLStmt SELECT_LAST_EARLIER_TRANSACTION = new SQLStmt(
        "SELECT * FROM wager_state WHERE wager_round_id = ? AND wager_id = ? ORDER BY state"
    );

    public static final byte REQUEST_MONEY = (byte) 1;
    public static final byte GOT_MONEY = (byte) 2;
    public static final byte GOT_OUTCOME = (byte) 3;
    public static final byte OUTCOME_CONFIRMED = (byte) 4;
    public static final byte ARCHIVED = (byte) 5;

    // TODO: this should be an enum that is shared between service and data store, but that requires a little black magic..
    static final Map<String, Byte> STATES =
        ImmutableMap.<String, Byte>builder()
        .put("REQUEST_MONEY", REQUEST_MONEY)
        .put("GOT_MONEY", GOT_MONEY)
        .put("GOT_OUTCOME", GOT_OUTCOME)
        .put("OUTCOME_CONFIRMED", OUTCOME_CONFIRMED)
        .put("ARCHIVED", ARCHIVED)
        .build();

    public static final byte SUCCESS = 0;
    public static final byte INVALID_TRANSITION = -1;


    public VoltTable[] run(long wagerRoundId, long wagerId, String transitionTypeName, long amount, long gameId, long exchangeRateId) {
        // TODO: need to solve issue of having to write information about game id, etc., to the wager_round table.
        // one solution is to always include those parameters as arguments, and in the beginning of this procedure to
        // check if the wager_round exists, and create it if not.

        byte state = STATES.get(transitionTypeName);

        if (state == REQUEST_MONEY) {
            insertWagerRoundIfNeeded(wagerRoundId, gameId, exchangeRateId);
        }

        if (!validTransition(wagerRoundId, wagerId, state)) {
            setAppStatusCode(INVALID_TRANSITION);
            setAppStatusString(String.format("got a %s without a corresponding %s", stateName(state), stateName((byte) (state -1))));
            return new VoltTable[0];
        }

        voltQueueSQL(INSERT, wagerRoundId, wagerId, state, amount, getTransactionTime().getTime());
        setAppStatusCode(SUCCESS);

        return voltExecuteSQL(true);
    }

    private String stateName(byte state) {
        for (Map.Entry<String, Byte> entry : STATES.entrySet()) {
            if (entry.getValue() == state) {
                return entry.getKey();
            }
        }

        return "INVALID STATE: " + state;
    }

    private boolean validTransition(long wagerRoundId, long wagerId, byte state) {
        voltQueueSQL(SELECT_LAST_EARLIER_TRANSACTION, wagerRoundId, wagerId);
        VoltTable[] tables = voltExecuteSQL();

        VoltTable actualTable = tables[0];

        byte previousState = 0;

        while (actualTable.advanceRow()) {
            previousState = (byte) actualTable.getLong("state");
        }

        return previousState == state -1;
    }

    private void insertWagerRoundIfNeeded(long wagerRoundId, long gameId, long exchangeRateId) {
        voltQueueSQL(INSERT_WAGER_ROUND, wagerRoundId, gameId, exchangeRateId);
        voltExecuteSQL(); // so as not to get this table in later results..
    }
}
