package com.williamsinteractive.casino.wager.procedures;

import com.google.common.base.Function;
import org.voltdb.VoltTable;

import javax.annotation.Nullable;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class FirstLong implements Function<VoltTable, Object[]> {
    @Nullable
    public Object[] apply(VoltTable input) {
        return new Object[]{input.getLong(0)};
    }
}
