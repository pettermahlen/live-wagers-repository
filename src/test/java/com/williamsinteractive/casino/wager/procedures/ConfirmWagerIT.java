package com.williamsinteractive.casino.wager.procedures;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.voltdb.VoltTable;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class ConfirmWagerIT extends VoltDbTestSupport {
    @Test
    public void shouldRecordGotMoneyWhenNormal() throws Exception {
        prepareWagerRound();
        prepareWager();

//        recordTransition("GOT_MONEY");

        VoltTable wager = readAllData("WAGER_SELECT_ALL");

        verifyTable(wager, ImmutableList.of(REQUEST_MONEY_ROW, GOT_MONEY_ROW));
    }

}
