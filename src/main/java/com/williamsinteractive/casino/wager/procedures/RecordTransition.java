package com.williamsinteractive.casino.wager.procedures;

import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */

public class RecordTransition extends VoltProcedure {
    public VoltTable[] run(long wagerRoundId, String transitionName) {
        return new VoltTable[0];
    }
}
