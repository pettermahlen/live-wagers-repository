package com.williamsinteractive.casino.wager.procedures;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class ConfirmWagerIT extends VoltDbTestSupport {
    @Test
    public void shouldConfirmWagerWhenNormal() throws Exception {
        prepareWagerRound();
        prepareWager();

        confirmWager();

        VoltTable wager = readAllData("WAGER_SELECT_ALL");

        verifyTable(wager, ImmutableList.of(GOT_MONEY_ROW));
    }

    @Test
    public void shouldReportSuccessWhenNormal() throws Exception {
        prepareWagerRound();
        prepareWager();

        ClientResponse response = confirmWager();

        assertThat(response.getAppStatus(), equalTo(ConfirmWager.SUCCESS));
    }

    @Test
    public void shouldFailIfNoWagerExists() throws Exception {
        prepareWagerRound();

        ClientResponse response = confirmWager();

        assertThat(response.getAppStatus(), equalTo(ConfirmWager.NO_SUCH_WAGER));
        assertThat(response.getAppStatusString(), containsString("No wager found for wager round id " + WAGER_ROUND_ID + " and wager id " + WAGER_ID));
    }

    @Test
    public void shouldFailIfNoWagerRoundExists() throws Exception {
        ClientResponse response = confirmWager();

        // not trying to distinguish the 'missing wager round' from the 'missing wager' case - it doesn't help much..
        assertThat(response.getAppStatus(), equalTo(ConfirmWager.NO_SUCH_WAGER));
        assertThat(response.getAppStatusString(), containsString("No wager found for wager round id " + WAGER_ROUND_ID + " and wager id " + WAGER_ID));
    }

    @Test
    public void shouldFailIfAlreadyConfirmed() throws Exception {
        prepareWagerRound();
        prepareWager();
        confirmWager();

        // and confirm again
        ClientResponse response = confirmWager();

        assertThat(response.getAppStatus(), equalTo(ConfirmWager.DUPLICATE_CONFIRMATION));
        assertThat(response.getAppStatusString(), containsString("Wager with wager round id " + WAGER_ROUND_ID + " and wager id " + WAGER_ID + " already confirmed"));
    }


    private ClientResponse confirmWager() throws IOException, ProcCallException {
        ClientResponse response = client.callProcedure("ConfirmWager", WAGER_ROUND_ID, WAGER_ID);

        assertThat(response, isSuccess());
        return response;
    }
}
