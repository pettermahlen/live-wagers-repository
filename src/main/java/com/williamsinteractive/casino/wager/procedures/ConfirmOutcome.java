package com.williamsinteractive.casino.wager.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class ConfirmOutcome extends VoltProcedure {
    private static final SQLStmt SELECT_WAGERS = new SQLStmt(
        "SELECT * FROM wager WHERE wager_round_id = ? ORDER BY wager_id"
    );
    private static final SQLStmt SELECT = new SQLStmt(
        "SELECT * FROM wager_round WHERE wager_round_id = ?"
    );
    private static final SQLStmt UPDATE = new SQLStmt(
        "UPDATE wager_round SET outcome_timestamp = ? WHERE wager_round_id = ?"
    );

    public static final byte SUCCESS = 0;
    public static final byte DUPLICATE_CONFIRMATION = 1;
    public static final byte MISSING_OUTCOME = 2;

    private static final String[] ERROR_MESSAGES = new String[]{
        "SUCCESS",
        "Wager round with id %d already confirmed",
        "Wager round with id %d has no outcome"
    };

    public VoltTable[] run(long wagerRoundId, long winAmount) {
        byte errorCode = validate(wagerRoundId);

        setAppStatusCode(errorCode);

        if (errorCode != SUCCESS) {
            setAppStatusString(String.format(ERROR_MESSAGES[errorCode], wagerRoundId));
            return new VoltTable[0];
        }

        voltQueueSQL(UPDATE, getTransactionTime(), wagerRoundId);
        voltExecuteSQL();

        voltQueueSQL(SELECT_WAGERS, wagerRoundId);
        voltQueueSQL(SELECT, wagerRoundId);
        return voltExecuteSQL(true);
    }

    private byte validate(long wagerRoundId) {
        voltQueueSQL(SELECT, wagerRoundId);
        VoltTable[] results = voltExecuteSQL();

        VoltTable wagers = results[0];

        if (wagers.getRowCount() == 0) {
            return MISSING_OUTCOME;
        }

        wagers.advanceRow();
        if (wagers.getLong("outcome_amount") == VoltType.NULL_BIGINT) {
            return MISSING_OUTCOME;
        }

        if (wagers.get("outcome_timestamp", VoltType.TIMESTAMP) != null) {
            return DUPLICATE_CONFIRMATION;
        }

        return SUCCESS;
    }
}
