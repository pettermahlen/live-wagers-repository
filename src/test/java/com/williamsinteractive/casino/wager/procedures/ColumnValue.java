package com.williamsinteractive.casino.wager.procedures;

import org.voltdb.VoltType;

/**
* TODO: document!
*
* @author Petter Måhlén
*/
class ColumnValue {
    String column;
    VoltType type;
    Object value;

    ColumnValue(String column, VoltType type, Object value) {
        this.column = column;
        this.type = type;
        this.value = value;
    }
}
