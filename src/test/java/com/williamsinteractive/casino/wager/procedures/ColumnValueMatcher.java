package com.williamsinteractive.casino.wager.procedures;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.voltdb.VoltTable;

import java.util.Objects;

/**
* TODO: document!
*
* @author Petter Måhlén
*/
class ColumnValueMatcher extends TypeSafeMatcher<VoltTable> {
    private final ColumnValue columnValue;

    ColumnValueMatcher(ColumnValue columnValue) {
        this.columnValue = columnValue;
    }

    @Override
    protected boolean matchesSafely(VoltTable item) {
        return Objects.equals(item.get(columnValue.column, columnValue.type), columnValue.value);
    }

    public void describeTo(Description description) {
        description.appendText("expected a column named '" + columnValue.column + "' with value: '" + columnValue.value);
    }
}
