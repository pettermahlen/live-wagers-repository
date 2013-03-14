package com.williamsinteractive.casino.wager.procedures;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.Map;

import static com.williamsinteractive.casino.wager.procedures.RecordWagerTransition.STATES;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class RecordTransitionIT extends VoltDbTestSupport {


    Map<String, Long> expectedRow;


    @Test
    public void shouldRecordRequestMoneyWhenNormal() throws Exception {
        ClientResponse response = recordTransition("REQUEST_MONEY");
        assertThat(response.getAppStatusString(), response.getAppStatus(), equalTo((byte) 0));

        VoltTable wager = readAllData("WAGER_STATE_SELECT_ALL");

        verifyTable(wager, ImmutableList.of(REQUEST_MONEY_ROW));
    }

    @Test
    public void shouldRecordGotMoneyWhenNormal() throws Exception {
        prepareWagerRound();
        prepareTransition("REQUEST_MONEY");

        recordTransition("GOT_MONEY");

        VoltTable wager = readAllData("WAGER_STATE_SELECT_ALL");

        verifyTable(wager, ImmutableList.of(REQUEST_MONEY_ROW, GOT_MONEY_ROW));
    }

    @Test
    public void shouldRecordGotOutcomeWhenNormal() throws Exception {
        prepareWagerRound();
        prepareTransition("REQUEST_MONEY");
        prepareTransition("GOT_MONEY");

        recordTransition("GOT_OUTCOME");

        VoltTable wager = readAllData("WAGER_STATE_SELECT_ALL");

        verifyTable(wager, ImmutableList.of(REQUEST_MONEY_ROW, GOT_MONEY_ROW, GOT_OUTCOME_ROW));
    }

    @Test
    public void shouldRecordOutcomeConfirmedWhenNormal() throws Exception {
        prepareWagerRound();
        prepareTransition("REQUEST_MONEY");
        prepareTransition("GOT_MONEY");
        prepareTransition("GOT_OUTCOME");

        recordTransition("OUTCOME_CONFIRMED");

        VoltTable wager = readAllData("WAGER_STATE_SELECT_ALL");

        verifyTable(wager, ImmutableList.of(REQUEST_MONEY_ROW, GOT_MONEY_ROW, GOT_OUTCOME_ROW, OUTCOME_CONFIRMED_ROW));
    }

    @Test
    public void shouldCreateWagerRoundIfMissingForRequestMoney() throws Exception {
        recordTransition("REQUEST_MONEY");

        VoltTable wagerRound = readAllData("WAGER_ROUND_SELECT_ALL");

        expectedRow = ImmutableMap.of("wager_round_id", WAGER_ROUND_ID,
                                      "game_id", GAME_ID,
                                      "exchange_rate_id", EXCHANGE_RATE_ID);
        verifyTable(wagerRound, ImmutableList.of(expectedRow));
    }

    @Test
    public void shouldFailGotMoneyIfNoRequestMoney() throws Exception {
        verifyTransitionIsInvalid("GOT_MONEY", "REQUEST_MONEY");
    }

    @Test
    public void shouldFailGotOutcomeIfNoGotMoney() throws Exception {
        prepareTransition("REQUEST_MONEY");

        verifyTransitionIsInvalid("GOT_OUTCOME", "GOT_MONEY");
    }

    @Test
    public void shouldFailOutcomeConfirmedIfNoGotOutcome() throws Exception {
        prepareTransition("REQUEST_MONEY");
        prepareTransition("GOT_MONEY");

        verifyTransitionIsInvalid("OUTCOME_CONFIRMED", "GOT_OUTCOME");
    }

    private void verifyTransitionIsInvalid(String toState, String requiredPrevious) throws Exception {
        prepareWagerRound();

        ClientResponse response = recordTransition(toState);

        String expectedErrorMessage = String.format("%s without a corresponding %s", toState, requiredPrevious);
        assertThat(response.getAppStatus(), equalTo(RecordWagerTransition.INVALID_TRANSITION));
        assertThat(response.getAppStatusString(), containsString(expectedErrorMessage));
    }

    private ClientResponse recordTransition(String newState) throws IOException, ProcCallException {
        ClientResponse response = client.callProcedure("RecordWagerTransition",
                                                       WAGER_ROUND_ID,
                                                       WAGER_ID,
                                                       newState,
                                                       AMOUNT,
                                                       GAME_ID,
                                                       EXCHANGE_RATE_ID);

        assertThat(response, isSuccess());
        return response;
    }


    static final Map<String, Long> OUTCOME_CONFIRMED_ROW = ImmutableMap.of("wager_round_id",
                                                                           WAGER_ROUND_ID,
                                                                           "wager_id",
                                                                           WAGER_ID,
                                                                           "amount",
                                                                           AMOUNT,
                                                                           "state",
                                                                           Long.valueOf(STATES.get("OUTCOME_CONFIRMED")));
}
