package com.williamsinteractive.casino.wager.procedures;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.ClientResponse;

import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
        prepareTransition("REQUEST_MONEY");
        prepareTransition("GOT_MONEY");
        prepareTransition("GOT_OUTCOME");

        recordOutcome(OUTCOME_AMOUNT);

        VoltTable wagerRound = readAllData("WAGER_ROUND_SELECT_ALL");

        verifyTable(wagerRound, ImmutableList.of(WAGER_ROUND_WITH_OUTCOME));
    }

    @Test
    public void shouldFailIfAnOutcomeExists() throws Exception {
        prepareWagerRound();
        prepareTransition("REQUEST_MONEY");
        prepareTransition("GOT_MONEY");
        prepareTransition("GOT_OUTCOME");
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

    @Test
    public void shouldReturnAllTransitionsAndTheWagerRound() throws Exception {
        prepareWagerRound();
        prepareTransition("REQUEST_MONEY");
        prepareTransition("GOT_MONEY");
        prepareTransition("GOT_OUTCOME");

        ClientResponse response = recordOutcome(OUTCOME_AMOUNT);

        assertThat(response.getAppStatus(), equalTo(RecordOutcome.SUCCESS));

        VoltTable[] tables = response.getResults();

        assertThat(tables.length, equalTo(2));

        VoltTable wagers = tables[0];
        VoltTable wagerRound = tables[1];

        verifyTable(wagers, ImmutableList.of(REQUEST_MONEY_ROW, GOT_MONEY_ROW, GOT_OUTCOME_ROW));
        verifyTable(wagerRound, ImmutableList.of(WAGER_ROUND_WITH_OUTCOME));

        wagerRound.resetRowPosition();
        wagerRound.advanceRow();
        assertThat(wagerRound.get("archive_timestamp", VoltType.TIMESTAMP), is(nullValue()));
    }

    private ClientResponse recordOutcome(long amount) throws Exception {
        ClientResponse response = client.callProcedure("RecordOutcome", WAGER_ROUND_ID, amount);

        assertThat(response, isSuccess());
        return response;
    }

    private static final Map<String, Long> WAGER_ROUND_WITH_OUTCOME =
        ImmutableMap.of("wager_round_id", WAGER_ROUND_ID,
                        "outcome_amount", OUTCOME_AMOUNT);
}
