package com.williamsinteractive.casino.wager.procedures;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class RecordOutcomeIT extends VoltDbTestSupport {

    @Test
    public void shouldRecordOutcomeWhenNormal() throws Exception {
        prepareWagerRound();
        completeWager();

        recordOutcome(OUTCOME_AMOUNT);

        VoltTable wagerRound = readAllData("WAGER_ROUND_SELECT_ALL");

        verifyTable(wagerRound, ImmutableList.of(WAGER_ROUND_WITH_OUTCOME));
    }

    @Test
    public void shouldFailIfAnOutcomeExists() throws Exception {
        prepareWagerRound();
        completeWager();
        prepareOutcome();

        ClientResponse response = recordOutcome(OUTCOME_AMOUNT);

        assertThat("duplicate", response.getAppStatus(), equalTo(RecordOutcome.DUPLICATE_OUTCOME));
        assertThat(response.getAppStatusString(), containsString("Duplicate outcome reported"));
        assertThat(response.getAppStatusString(), containsString(String.valueOf(WAGER_ROUND_ID)));
    }

    @Test
    public void shouldFailIfNoWagerExists() throws Exception {
        ClientResponse response = recordOutcome(OUTCOME_AMOUNT);

        assertThat("missing", response.getAppStatus(), equalTo(RecordOutcome.NO_SUCH_WAGER));
        assertThat(response.getAppStatusString(), containsString("No wager found"));
        assertThat(response.getAppStatusString(), containsString(String.valueOf(WAGER_ROUND_ID)));
    }


    private ClientResponse recordOutcome(long amount) throws Exception {
        ClientResponse response = client.callProcedure("RecordOutcome", WAGER_ROUND_ID, amount);

        assertThat(response, isSuccess());
        return response;
    }

}
