package com.williamsinteractive.casino.wager.procedures;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.williamsinteractive.casino.wager.procedures.RecordWagerTransition.STATES;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class RecordTransitionIT {
    private static final long WAGER_ROUND_ID = 2376;
    private static final long WAGER_ID = 8765;
    private static final long GAME_ID = 87436523;
    private static final long EXCHANGE_RATE_ID = 543543;
    private static final long AMOUNT = 675323;
    private static final long DUMMY_TIMESTAMP = 987231;


    protected Client client;
    Map<String, Long> expectedRow;

    @Before
    public void setUp() throws Exception {
        ClientConfig config = new ClientConfig("test", "test");
        client = ClientFactory.createClient(config);
        client.createConnection("localhost");

        clearTable("wager_round", new FirstLong());
        clearTable("wager_state", new WagerStatePK());
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }


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

    private void prepareWagerRound() throws Exception {
        ClientResponse response = client.callProcedure("WAGER_ROUND.insert",
                                                       WAGER_ROUND_ID,
                                                       GAME_ID,
                                                       EXCHANGE_RATE_ID,
                                                       null,
                                                       null);

        assertThat(response, isSuccess());
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

    private void prepareTransition(String transitionName) throws Exception {
        ClientResponse response = client.callProcedure("WAGER_STATE.insert",
                                                       WAGER_ROUND_ID,
                                                       WAGER_ID,
                                                       RecordWagerTransition.STATES.get(transitionName),
                                                       AMOUNT,
                                                       DUMMY_TIMESTAMP);

        assertThat(response, isSuccess());
    }


    private VoltTable readAllData(String selectAllProcedure) throws Exception {
        ClientResponse response = client.callProcedure(selectAllProcedure);

        assertThat(response, isSuccess());

        return response.getResults()[0];
    }

    private void verifyTable(VoltTable table, List<Map<String, Long>> expectedRows) {
        assertThat(table.getRowCount(), equalTo(expectedRows.size()));

        for (Map<String, Long> expectedRow : expectedRows) {
            table.advanceRow();

            for (String key : expectedRow.keySet()) {
                assertThat("key: " + key, table.getLong(key), equalTo(expectedRow.get(key)));
            }
        }
    }

    private Matcher<? super ClientResponse> isSuccess() {
        return new ClientResponseSuccess();
    }

    private static class WagerStatePK implements Function<VoltTable, Object[]> {
        @Nullable
        public Object[] apply(VoltTable input) {
            return new Object[]{input.getLong(0),
                                input.getLong(1),
                                input.getLong(2)};
        }
    }


    protected void clearTable(String tableName,
                              Function<VoltTable, Object[]> primaryKeyFunction) throws IOException, ProcCallException {
        String upperCaseTable = tableName.toUpperCase();

        VoltTable table = client.callProcedure(upperCaseTable + "_SELECT_ALL").getResults()[0];

        while (table.advanceRow()) {
            client.callProcedure(upperCaseTable + ".delete", primaryKeyFunction.apply(table));
        }
    }


    static final Map<String, Long> REQUEST_MONEY_ROW = ImmutableMap.of("wager_round_id",
                                                                       WAGER_ROUND_ID,
                                                                       "wager_id",
                                                                       WAGER_ID,
                                                                       "amount",
                                                                       AMOUNT,
                                                                       "state",
                                                                       Long.valueOf(STATES.get("REQUEST_MONEY")));
    static final Map<String, Long> GOT_MONEY_ROW = ImmutableMap.of("wager_round_id",
                                                                   WAGER_ROUND_ID,
                                                                   "wager_id",
                                                                   WAGER_ID,
                                                                   "amount",
                                                                   AMOUNT,
                                                                   "state",
                                                                   Long.valueOf(STATES.get("GOT_MONEY")));
    static final Map<String, Long> GOT_OUTCOME_ROW = ImmutableMap.of("wager_round_id",
                                                                     WAGER_ROUND_ID,
                                                                     "wager_id",
                                                                     WAGER_ID,
                                                                     "amount",
                                                                     AMOUNT,
                                                                     "state",
                                                                     Long.valueOf(STATES.get("GOT_OUTCOME")));
    static final Map<String, Long> OUTCOME_CONFIRMED_ROW = ImmutableMap.of("wager_round_id",
                                                                           WAGER_ROUND_ID,
                                                                           "wager_id",
                                                                           WAGER_ID,
                                                                           "amount",
                                                                           AMOUNT,
                                                                           "state",
                                                                           Long.valueOf(STATES.get("OUTCOME_CONFIRMED")));
}
