package com.williamsinteractive.casino.wager.procedures;

import org.voltdb.VoltType;

/**
* TODO: document!
*
* @author Petter Måhlén
*/
class BigIntMatcher extends ColumnValueMatcher {
    BigIntMatcher(String column, long value) {
        super(new ColumnValue(column, VoltType.BIGINT, value));
    }
}
