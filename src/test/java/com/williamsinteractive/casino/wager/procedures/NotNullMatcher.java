package com.williamsinteractive.casino.wager.procedures;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;

/**
* TODO: document!
*
* @author Petter Måhlén
*/
class NotNullMatcher extends TypeSafeMatcher<VoltTable> {
    private final String column;
    private final VoltType type;

    NotNullMatcher(String column, VoltType type) {
        this.column = column;
        this.type = type;
    }

    @Override
    protected boolean matchesSafely(VoltTable item) {
        return item.get(column, type) != null;
    }

    public void describeTo(Description description) {
        description.appendText("Not 'null' for column '" + column + "'");
    }
}
