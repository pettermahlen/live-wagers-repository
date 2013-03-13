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
    private final SQLStmt INSERT = new SQLStmt(
        "INSERT INTO wager_round_transaction (wager_round_id, transaction_id, transaction_type, amount, transaction_time)" +
            " VALUES (?, ?, ?, ?, ?)");

    // TODO: this should be an enum that is shared between service and data store, but that requires a little black magic..
    private static final Map<String, Integer> TRANSITION_TYPES =
        ImmutableMap.<String, Integer>builder()
        .put("REQUEST_MONEY", 1)
        .put("GOT_MONEY", 2)
        .put("GOT_OUTCOME", 3)
        .put("OUTCOME_CONFIRMED", 4)
        .put("ARCHIVED", 5)
        .build();


    public VoltTable[] run(long wagerRoundId, long transactionId, String transitionTypeName, long amount) {
        // TODO: need to solve issue of having to write information about game id, etc., to the wager_round table.
        // one solution is to always include those parameters as arguments, and in the beginning of this procedure to
        // check if the wager_round exists, and create it if not.

        voltQueueSQL(INSERT, wagerRoundId, transactionId, TRANSITION_TYPES.get(transitionTypeName), amount, getTransactionTime().getTime());

        return voltExecuteSQL(true);
    }
}
