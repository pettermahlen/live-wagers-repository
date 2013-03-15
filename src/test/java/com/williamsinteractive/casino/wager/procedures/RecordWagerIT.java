package com.williamsinteractive.casino.wager.procedures;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class RecordWagerIT extends VoltDbTestSupport {


    @Test
    public void shouldRecordRequestMoneyWhenNormal() throws Exception {
        ClientResponse response = recordWager();
        assertThat(response.getAppStatusString(), response.getAppStatus(), equalTo(RecordWager.SUCCESS));

        VoltTable wager = readAllData("WAGER_SELECT_ALL");

        verifyTable(wager, ImmutableList.of(REQUEST_MONEY_ROW));
    }

    @Test
    public void shouldCreateWagerRoundIfMissingForRequestMoney() throws Exception {
        recordWager();

        VoltTable wagerRound = readAllData("WAGER_ROUND_SELECT_ALL");

        verifyTable(wagerRound, ImmutableList.of(EXPECTED_WAGER_ROUND));
    }

    @Test
    public void shouldNotCreateWagerRoundIfMissingForRequestMoney() throws Exception {
        prepareWagerRound();
        recordWager();

        VoltTable wagerRound = readAllData("WAGER_ROUND_SELECT_ALL");

        verifyTable(wagerRound, ImmutableList.of(EXPECTED_WAGER_ROUND));
    }

    @Test
    public void shouldFailIfPreviousRowExists() throws Exception {
        prepareWager();
        ClientResponse response = recordWager();
        assertThat(response.getAppStatus(), equalTo(RecordWager.DUPLICATE_WAGER));
        assertThat(response.getAppStatusString(), containsString(String.valueOf(WAGER_ROUND_ID)));
        assertThat(response.getAppStatusString(), containsString(String.valueOf(WAGER_ID)));
    }

    @Test
    public void shouldFailIfThereIsAnOutcomeForTheWagerRound() throws Exception {

        fail("test not implemented");

    }

    private ClientResponse recordWager() throws IOException, ProcCallException {
        ClientResponse response = client.callProcedure("RecordWager",
                                                       WAGER_ROUND_ID,
                                                       WAGER_ID,
                                                       AMOUNT,
                                                       GAME_ID,
                                                       EXCHANGE_RATE_ID);

        assertThat(response, isSuccess());
        return response;
    }


    static final List<Matcher<VoltTable>> EXPECTED_WAGER_ROUND = ImmutableList.<Matcher<VoltTable>>of(
        new BigIntMatcher("wager_round_id", WAGER_ROUND_ID),
        new BigIntMatcher("game_id", GAME_ID),
        new BigIntMatcher("exchange_rate_id", EXCHANGE_RATE_ID)
    );
}
