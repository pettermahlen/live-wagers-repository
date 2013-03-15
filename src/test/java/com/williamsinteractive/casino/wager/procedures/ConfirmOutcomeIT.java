package com.williamsinteractive.casino.wager.procedures;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class ConfirmOutcomeIT extends VoltDbTestSupport {

    @Test
    public void shouldUpdateConfirmationDateNormally() throws Exception {
        prepareWagerRound();
        completeWager();
        prepareOutcome();

        confirmOutcome();

        VoltTable wagerRound = readAllData("WAGER_ROUND_SELECT_ALL");

        verifyTable(wagerRound, ImmutableList.of(WAGER_ROUND_WITH_CONFIRMED_OUTCOME));
    }

    @Test
    public void shouldReturnSuccessNormally() throws Exception {
        prepareWagerRound();
        completeWager();
        prepareOutcome();

        ClientResponse response = confirmOutcome();

        assertThat(response.getAppStatus(), equalTo(ConfirmOutcome.SUCCESS));
    }

    @Test
    public void shouldReturnAllTransitionsAndTheWagerRound() throws Exception {
        prepareWagerRound();
        completeWager();
        completeWager2();
        prepareOutcome();

        ClientResponse response = confirmOutcome();

        assertThat(response.getAppStatus(), equalTo(RecordOutcome.SUCCESS));

        VoltTable[] tables = response.getResults();

        assertThat(tables.length, equalTo(2));

        VoltTable wagers = tables[0];
        VoltTable wagerRound = tables[1];

        verifyTable(wagers, ImmutableList.of(GOT_MONEY_ROW, GOT_MONEY_ROW2));
        verifyTable(wagerRound, ImmutableList.of(WAGER_ROUND_WITH_CONFIRMED_OUTCOME));

        wagerRound.resetRowPosition();
        wagerRound.advanceRow();
        assertThat(wagerRound.get("archive_timestamp", VoltType.TIMESTAMP), is(nullValue()));
    }

    @Test
    public void shouldFailIfAlreadyConfirmed() throws Exception {
        prepareWagerRound();
        completeWager();
        prepareConfirmedOutcome();

        ClientResponse response = confirmOutcome();

        assertThat(response.getAppStatus(), equalTo(ConfirmOutcome.DUPLICATE_CONFIRMATION));
        assertThat(response.getAppStatusString(), containsString("Wager round with id " + WAGER_ROUND_ID + " already confirmed"));
    }

    @Test
    public void shouldFailIfNoOutcome() throws Exception {
        prepareWagerRound();
        completeWager();

        ClientResponse response = confirmOutcome();

        assertThat(response.getAppStatus(), equalTo(ConfirmOutcome.MISSING_OUTCOME));
        assertThat(response.getAppStatusString(), containsString("Wager round with id " + WAGER_ROUND_ID + " has no outcome"));
    }

    private ClientResponse confirmOutcome() throws IOException, ProcCallException {
        ClientResponse response = client.callProcedure("ConfirmOutcome", WAGER_ROUND_ID, OUTCOME_AMOUNT);

        assertThat(response, isSuccess());
        return response;
    }
}
