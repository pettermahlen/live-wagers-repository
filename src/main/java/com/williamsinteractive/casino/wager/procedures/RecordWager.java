package com.williamsinteractive.casino.wager.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */

public class RecordWager extends VoltProcedure {
    private final SQLStmt SELECT_WAGER_ROUND = new SQLStmt(
        "SELECT * FROM wager_round WHERE wager_round_id = ?"
    );
    private final SQLStmt INSERT_WAGER_ROUND = new SQLStmt(
        "INSERT INTO wager_round (wager_round_id, game_id, exchange_rate_id) VALUES (?, ?, ?)"
    );
    private final SQLStmt INSERT = new SQLStmt(
        "INSERT INTO wager (wager_round_id, wager_id, amount, created) VALUES (?, ?, ?, ?)"
    );
    private final SQLStmt SELECT = new SQLStmt(
        "SELECT * FROM wager WHERE wager_round_id = ? AND wager_id = ?"
    );

    public static final byte SUCCESS = 0;
    public static final byte DUPLICATE_WAGER = -1;


    public VoltTable[] run(long wagerRoundId, long wagerId, long amount, long gameId, long exchangeRateId) {
        insertWagerRoundIfNeeded(wagerRoundId, gameId, exchangeRateId);

        if (exists(wagerRoundId, wagerId)) {
            setAppStatusCode(DUPLICATE_WAGER);
            setAppStatusString(String.format("got a duplicate wager for wager round %d and wager %d", wagerRoundId, wagerId));
            return new VoltTable[0];
        }

        voltQueueSQL(INSERT, wagerRoundId, wagerId, amount, getTransactionTime().getTime());
        setAppStatusCode(SUCCESS);

        return voltExecuteSQL(true);
    }

    private boolean exists(long wagerRoundId, long wagerId) {
        voltQueueSQL(SELECT, wagerRoundId, wagerId);
        VoltTable[] tables = voltExecuteSQL();

        return tables[0].getRowCount() > 0;
    }

    private void insertWagerRoundIfNeeded(long wagerRoundId, long gameId, long exchangeRateId) {
        voltQueueSQL(SELECT_WAGER_ROUND, EXPECT_ZERO_OR_ONE_ROW, wagerRoundId);
        VoltTable[] selectResult = voltExecuteSQL();

        if (selectResult[0].getRowCount() == 0) {
            voltQueueSQL(INSERT_WAGER_ROUND, wagerRoundId, gameId, exchangeRateId);
            voltExecuteSQL();
        }
    }
}
