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
public class RecordArchival extends VoltProcedure {
    public static final byte SUCCESS = 0;
    public static final byte ALREADY_ARCHIVED = 1;
    public static final byte NO_WAGER = 2;
    public static final byte NO_OUTCOME = 3;

    private static final SQLStmt SELECT = new SQLStmt(
        "SELECT * FROM wager_round WHERE wager_round_id = ?"
    );
    private static final SQLStmt UPDATE = new SQLStmt(
        "UPDATE wager_round SET archive_timestamp = ? WHERE wager_round_id = ?"
    );

    private static final String[] ERROR_MESSAGES = new String[] {
        "SUCCESS",
        "wager_round_id %d has already been archived",
        "No wager found for wager_round_id %d",
        "No outcome found for wager_round_id %d",
    };

    public VoltTable[] run(long wagerRoundId) {
        byte errorCode = validate(wagerRoundId);

        setAppStatusCode(errorCode);

        if (errorCode != SUCCESS) {
            setAppStatusString(String.format(ERROR_MESSAGES[errorCode], wagerRoundId));
            return new VoltTable[0];
        }

        voltQueueSQL(UPDATE, getTransactionTime(), wagerRoundId);
        return voltExecuteSQL(true);
    }

    private byte validate(long wagerRoundId) {
        voltQueueSQL(SELECT, wagerRoundId);

        VoltTable result = voltExecuteSQL()[0];

        if (result.getRowCount() == 0) {
            return NO_WAGER;
        }

        result.advanceRow();

        if (result.get("outcome_timestamp", VoltType.TIMESTAMP) == null) {
            return NO_OUTCOME;
        }

        if (result.get("archive_timestamp", VoltType.TIMESTAMP) != null) {
            return ALREADY_ARCHIVED;
        }

        return SUCCESS;
    }
}
