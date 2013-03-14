package com.williamsinteractive.casino.wager.procedures;

import com.google.common.base.Function;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class VoltDbTestSupport {
    protected static final long WAGER_ROUND_ID = 2376;
    protected static final long WAGER_ID = 8765;
    protected static final long GAME_ID = 87436523;
    protected static final long EXCHANGE_RATE_ID = 543543;
    protected static final long AMOUNT = 675323;
    protected static final long DUMMY_TIMESTAMP = 987231;
    protected Client client;

    @Before
    public final void connectToVolt() throws Exception {
        ClientConfig config = new ClientConfig("test", "test");
        client = ClientFactory.createClient(config);
        client.createConnection("localhost");

        clearTable("wager_round", new FirstLong());
        clearTable("wager_state", new WagerStatePK());
    }

    @After
    public final void closeVolt() throws Exception {
        client.close();
    }

    protected void clearTable(String tableName,
                              Function<VoltTable, Object[]> primaryKeyFunction) throws IOException, ProcCallException {
        String upperCaseTable = tableName.toUpperCase();

        VoltTable table = client.callProcedure(upperCaseTable + "_SELECT_ALL").getResults()[0];

        while (table.advanceRow()) {
            client.callProcedure(upperCaseTable + ".delete", primaryKeyFunction.apply(table));
        }
    }

    protected void prepareWagerRound() throws Exception {
        ClientResponse response = client.callProcedure("WAGER_ROUND.insert",
                                                       WAGER_ROUND_ID,
                                                       GAME_ID,
                                                       EXCHANGE_RATE_ID,
                                                       null,
                                                       null);

        assertThat(response, isSuccess());
    }

    protected void prepareTransition(String transitionName) throws Exception {
        ClientResponse response = client.callProcedure("WAGER_STATE.insert",
                                                       WAGER_ROUND_ID,
                                                       WAGER_ID,
                                                       RecordWagerTransition.STATES.get(transitionName),
                                                       AMOUNT,
                                                       DUMMY_TIMESTAMP);

        assertThat(response, isSuccess());
    }

    protected Matcher<? super ClientResponse> isSuccess() {
        return new ClientResponseSuccess();
    }

    protected VoltTable readAllData(String selectAllProcedure) throws Exception {
        ClientResponse response = client.callProcedure(selectAllProcedure);

        assertThat(response, isSuccess());

        return response.getResults()[0];
    }

    protected void verifyTable(VoltTable table, List<Map<String, Long>> expectedRows) {
        assertThat(table.getRowCount(), equalTo(expectedRows.size()));

        for (Map<String, Long> expectedRow : expectedRows) {
            table.advanceRow();

            for (String key : expectedRow.keySet()) {
                assertThat("key: " + key, table.getLong(key), equalTo(expectedRow.get(key)));
            }
        }
    }

    private static class WagerStatePK implements Function<VoltTable, Object[]> {
        @Nullable
        public Object[] apply(VoltTable input) {
            return new Object[]{input.getLong(0),
                                input.getLong(1),
                                input.getLong(2)};
        }
    }
}
