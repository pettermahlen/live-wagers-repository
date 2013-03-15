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
public class RecordOutcome extends VoltProcedure {
    public static final byte SUCCESS = 0;
    public static final byte DUPLICATE_OUTCOME = 1;
    public static final byte NO_SUCH_WAGER = 2;

    private static final SQLStmt SELECT = new SQLStmt(
        "SELECT * FROM wager_round WHERE wager_round_id = ?"
    );
    private static final SQLStmt UPDATE = new SQLStmt(
        "UPDATE wager_round SET outcome_amount = ? WHERE wager_round_id = ?"
    );

    private static final String[] ERROR_MESSAGES = new String[] {
        "SUCCESS",
        "Duplicate outcome reported for wager_round_id %d",
        "No wager found for wager_round_id %d"
    };

    public VoltTable[] run(long wagerRoundId, long amount) {
        byte errorCode = validate(wagerRoundId);

        setAppStatusCode(errorCode);

        if (errorCode != SUCCESS) {
            setAppStatusString(String.format(ERROR_MESSAGES[errorCode], wagerRoundId));
            return new VoltTable[0];
        }

        voltQueueSQL(UPDATE, EXPECT_ONE_ROW, amount, wagerRoundId);
        return voltExecuteSQL();
    }

    private byte validate(long wagerRoundId) {
        voltQueueSQL(SELECT, wagerRoundId);
        VoltTable[] tables = voltExecuteSQL();

        VoltTable result = tables[0];

        if (result.getRowCount() == 0) {
            return NO_SUCH_WAGER;
        }

        result.advanceRow();
        if (!VoltType.BIGINT.getNullValue().equals(result.getLong("outcome_amount"))) {
            return DUPLICATE_OUTCOME;
        }

        return SUCCESS;
    }
}
