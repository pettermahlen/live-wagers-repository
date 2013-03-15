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
public class ConfirmWager extends VoltProcedure {
    public static final byte SUCCESS = 0;
    public static final byte NO_SUCH_WAGER = 1;
    public static final byte DUPLICATE_CONFIRMATION = 2;

    private static final String[] ERROR_MESSAGES = new String[] {
        "SUCCESS",
        "No wager found for wager round id %d and wager id %d",
        "Wager with wager round id %d and wager id %d already confirmed"
    };

    private static final SQLStmt UPDATE = new SQLStmt(
        "UPDATE wager SET confirmed = ? WHERE wager_round_id = ? AND wager_id = ?"
    );

    private static final SQLStmt SELECT = new SQLStmt(
        "SELECT * FROM wager WHERE wager_round_id = ? AND wager_id = ?"
    );

    public VoltTable[] run(long wagerRoundId, long wagerId) {
        byte errorCode = validate(wagerRoundId, wagerId);

        setAppStatusCode(errorCode);

        if (errorCode != SUCCESS) {
            setAppStatusString(String.format(ERROR_MESSAGES[errorCode], wagerRoundId, wagerId));
            return new VoltTable[0];
        }

        voltQueueSQL(UPDATE, getTransactionTime(), wagerRoundId, wagerId);

        return voltExecuteSQL();
    }

    private byte validate(long wagerRoundId, long wagerId) {
        voltQueueSQL(SELECT, wagerRoundId, wagerId);
        VoltTable[] result = voltExecuteSQL();

        VoltTable wager = result[0];

        if (wager.getRowCount() == 0) {
            return NO_SUCH_WAGER;
        }

        wager.advanceRow();

        if (wager.get("confirmed", VoltType.TIMESTAMP) != null) {
            return DUPLICATE_CONFIRMATION;
        }

        return SUCCESS;
    }
}
