package com.williamsinteractive.casino.wager.procedures;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class VoltDbTestSupport {
    protected static final long OUTCOME_AMOUNT = 8796123L;
    protected static final long WAGER_ROUND_ID = 2376;
    protected static final long WAGER_ID = 8765;
    protected static final long WAGER_ID2 = 34655;
    protected static final long GAME_ID = 87436523;
    protected static final long EXCHANGE_RATE_ID = 543543;
    protected static final long AMOUNT = 675323;
    protected static final long AMOUNT2 = 2354;
    protected static final long DUMMY_TIMESTAMP = 987231;

    protected static final List<Matcher<VoltTable>> WAGER_ROUND_WITH_OUTCOME =
        ImmutableList.<Matcher<VoltTable>>of(
            new BigIntMatcher("wager_round_id", WAGER_ROUND_ID),
            new BigIntMatcher("outcome_amount", OUTCOME_AMOUNT),
            new ColumnValueMatcher(new ColumnValue("outcome_timestamp", VoltType.TIMESTAMP, null)),
            new ColumnValueMatcher(new ColumnValue("archive_timestamp", VoltType.TIMESTAMP, null))
        );
    protected static final List<Matcher<VoltTable>> WAGER_ROUND_WITH_CONFIRMED_OUTCOME =
        ImmutableList.<Matcher<VoltTable>>of(
            new BigIntMatcher("wager_round_id", WAGER_ROUND_ID),
            new BigIntMatcher("outcome_amount", OUTCOME_AMOUNT),
            new NotNullMatcher("outcome_timestamp", VoltType.TIMESTAMP),
            new ColumnValueMatcher(new ColumnValue("archive_timestamp", VoltType.TIMESTAMP, null))
        );

    static final List<Matcher<VoltTable>> REQUEST_MONEY_ROW = ImmutableList.<Matcher<VoltTable>>of(
        new BigIntMatcher("wager_round_id", WAGER_ROUND_ID),
        new BigIntMatcher("wager_id", WAGER_ID),
        new BigIntMatcher("amount", AMOUNT),
        new NotNullMatcher("created", VoltType.TIMESTAMP),
        new ColumnValueMatcher(new ColumnValue("confirmed", VoltType.TIMESTAMP, null))
    );
    static final List<Matcher<VoltTable>> GOT_MONEY_ROW = ImmutableList.<Matcher<VoltTable>>of(
        new BigIntMatcher("wager_round_id", WAGER_ROUND_ID),
        new BigIntMatcher("wager_id", WAGER_ID),
        new BigIntMatcher("amount", AMOUNT),
        new NotNullMatcher("created", VoltType.TIMESTAMP),
        new NotNullMatcher("confirmed", VoltType.TIMESTAMP)
    );
    static final List<Matcher<VoltTable>> GOT_MONEY_ROW2 = ImmutableList.<Matcher<VoltTable>>of(
        new BigIntMatcher("wager_round_id", WAGER_ROUND_ID),
        new BigIntMatcher("wager_id", WAGER_ID2),
        new BigIntMatcher("amount", AMOUNT2),
        new NotNullMatcher("created", VoltType.TIMESTAMP),
        new NotNullMatcher("confirmed", VoltType.TIMESTAMP)
    );

    //    static final Map<String, Predicate<ColumnValue>> GOT_MONEY_ROW =
    //        ImmutableMap.of("wager_round_id", Predicates.equalTo(new ColumnValue(VoltType.BIGINT, WAGER_ROUND_ID)),
    //                        "wager_id", Predicates.equalTo(new ColumnValue(VoltType.BIGINT, WAGER_ID)),
    //                        "amount", Predicates.equalTo(new ColumnValue(VoltType.BIGINT, AMOUNT)),
    //                        "created", Predicates.<ColumnValue>notNull());

    protected Client client;

    @Before
    public final void connectToVolt() throws Exception {
        ClientConfig config = new ClientConfig("test", "test");
        client = ClientFactory.createClient(config);
        client.createConnection("localhost");

        clearTable("wager_round", new FirstLong());
        clearTable("wager", new WagerPK());
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
                                                       null,
                                                       null);

        assertThat(response, isSuccess());
    }

    protected void prepareWager() throws Exception {
        ClientResponse response = client.callProcedure("WAGER.insert",
                                                       WAGER_ROUND_ID,
                                                       WAGER_ID,
                                                       AMOUNT,
                                                       DUMMY_TIMESTAMP,
                                                       null);

        assertThat(response, isSuccess());
    }

    protected void completeWager() throws Exception {
        ClientResponse response = client.callProcedure("WAGER.insert",
                                                       WAGER_ROUND_ID,
                                                       WAGER_ID,
                                                       AMOUNT,
                                                       DUMMY_TIMESTAMP,
                                                       DUMMY_TIMESTAMP);

        assertThat(response, isSuccess());
    }

    protected void completeWager2() throws Exception {
        ClientResponse response = client.callProcedure("WAGER.insert",
                                                       WAGER_ROUND_ID,
                                                       WAGER_ID2,
                                                       AMOUNT2,
                                                       DUMMY_TIMESTAMP,
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

    protected void verifyTable(VoltTable table, List<List<Matcher<VoltTable>>> expectedRows) {
        assertThat(table.getRowCount(), equalTo(expectedRows.size()));

        for (List<Matcher<VoltTable>> expectedRow : expectedRows) {
            table.advanceRow();

            for (Matcher<VoltTable> matcher : expectedRow) {
                assertThat(table, matcher);
            }
        }
    }

    protected void prepareOutcome() throws IOException, ProcCallException {
        ClientResponse response = client.callProcedure("WAGER_ROUND.update",
                                                       WAGER_ROUND_ID,
                                                       GAME_ID,
                                                       EXCHANGE_RATE_ID,
                                                       OUTCOME_AMOUNT,
                                                       null,
                                                       null,
                                                       WAGER_ROUND_ID);

        assertThat(response, isSuccess());
    }

    protected void prepareConfirmedOutcome() throws IOException, ProcCallException {
        ClientResponse response = client.callProcedure("WAGER_ROUND.update",
                                                       WAGER_ROUND_ID,
                                                       GAME_ID,
                                                       EXCHANGE_RATE_ID,
                                                       OUTCOME_AMOUNT,
                                                       DUMMY_TIMESTAMP,
                                                       null,
                                                       WAGER_ROUND_ID);

        assertThat(response, isSuccess());
    }

    protected void prepareArchival() throws IOException, ProcCallException {
        ClientResponse response = client.callProcedure("WAGER_ROUND.update",
                                                       WAGER_ROUND_ID,
                                                       GAME_ID,
                                                       EXCHANGE_RATE_ID,
                                                       AMOUNT,
                                                       DUMMY_TIMESTAMP,
                                                       DUMMY_TIMESTAMP,
                                                       WAGER_ROUND_ID);

        assertThat(response, isSuccess());
    }

    private static class WagerPK implements Function<VoltTable, Object[]> {
        @Nullable
        public Object[] apply(VoltTable input) {
            return new Object[]{input.getLong(0),
                                input.getLong(1)};
        }
    }

}
