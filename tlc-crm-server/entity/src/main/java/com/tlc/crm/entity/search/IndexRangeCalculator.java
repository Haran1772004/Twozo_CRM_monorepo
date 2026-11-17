package com.tlc.crm.entity.search;

import com.tlc.commons.Counter;
import com.tlc.commons.util.constants.NumericalConstant;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * Calculates index's range based on the range.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class IndexRangeCalculator {

    private static class Instance {
        private static final IndexRangeCalculator INSTANCE = new IndexRangeCalculator();
    }

    private IndexRangeCalculator() {
    }

    public static IndexRangeCalculator getInstance() {
        return IndexRangeCalculator.Instance.INSTANCE;
    }


    /**
     * <p>
     * Calculates from` and `size` of an index for the given range.
     * </p>
     *
     * @param docCountMap Total document count for each index.
     * @param start       Start of the current range.
     * @param limit       End of the current range.
     * @return index's `from` and `count` in a Map.
     */
    public Map<String, Map<Integer, Integer>> calculate(final Map<String, Integer> docCountMap, final int start, final int limit) {
        final Map<String, Map<Integer, Integer>> indexRangeMap = new LinkedHashMap<>();
        final Counter cumulativeTotalCounter = new Counter();
        final Counter remainingLimitCounter = new Counter(limit);
        boolean isStartPoint = false;

        for (final Map.Entry<String, Integer> entry : docCountMap.entrySet()) {
            final String indexName = entry.getKey();
            final int totalDocCount = entry.getValue();

            if (isStartPoint) {
                final int fetchSize = Math.min(totalDocCount, remainingLimitCounter.getCount());

                if (NumericalConstant.ZERO < fetchSize) {
                    final Map<Integer, Integer> range = new HashMap<>();

                    range.put(NumericalConstant.ZERO, fetchSize);
                    indexRangeMap.put(indexName, range);
                    remainingLimitCounter.setCount(remainingLimitCounter.getCount() - fetchSize);
                }
                cumulativeTotalCounter.setCount(cumulativeTotalCounter.getCount() + totalDocCount);

                if (NumericalConstant.ZERO == remainingLimitCounter.getCount()) {
                    break;
                }
            } else {
                final int cumulative = cumulativeTotalCounter.getCount() + totalDocCount;

                if (cumulative < start) {
                    cumulativeTotalCounter.setCount(cumulative);
                } else {
                    isStartPoint = true;
                    final int fetchFrom = start - cumulativeTotalCounter.getCount() - NumericalConstant.ONE;
                    final int fetchSize = Math.min(totalDocCount - fetchFrom, remainingLimitCounter.getCount());

                    if (NumericalConstant.ZERO < fetchSize) {
                        final Map<Integer, Integer> rangeMap = new HashMap<>();

                        rangeMap.put(fetchFrom, fetchSize);
                        indexRangeMap.put(indexName, rangeMap);
                        remainingLimitCounter.setCount(remainingLimitCounter.getCount() - fetchSize);
                    }
                    cumulativeTotalCounter.setCount(cumulativeTotalCounter.getCount() + totalDocCount);

                    if (NumericalConstant.ZERO >= remainingLimitCounter.getCount()) {
                        break;
                    }
                }
            }
        }

        return indexRangeMap;
    }
}
