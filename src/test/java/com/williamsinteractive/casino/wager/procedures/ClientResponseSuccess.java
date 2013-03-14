package com.williamsinteractive.casino.wager.procedures;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.voltdb.client.ClientResponse;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class ClientResponseSuccess extends TypeSafeMatcher<ClientResponse> {
    @Override
    protected boolean matchesSafely(ClientResponse item) {
        return item.getStatus() == ClientResponse.SUCCESS;
    }

    public void describeTo(Description description) {
        description.appendText("with status == ClientResponse.SUCCESS");
    }
}
