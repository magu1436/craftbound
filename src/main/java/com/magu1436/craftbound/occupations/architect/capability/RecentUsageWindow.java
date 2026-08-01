package com.magu1436.craftbound.occupations.architect.capability;

import java.util.Arrays;

final class RecentUsageWindow {
    private long[] minuteBuckets;
    private long lastUpdatedMinute;

    RecentUsageWindow(int windowMinutes, long currentMinute) {
        minuteBuckets = new long[windowMinutes];
        lastUpdatedMinute = currentMinute;
    }

    RecentUsageWindow(long[] minuteBuckets, long lastUpdatedMinute) {
        this.minuteBuckets = minuteBuckets.clone();
        this.lastUpdatedMinute = lastUpdatedMinute;
    }

    int record(long currentMinute, int windowMinutes) {
        resizeIfNeeded(windowMinutes, currentMinute);
        advance(currentMinute);
        int index = Math.floorMod(currentMinute, minuteBuckets.length);
        minuteBuckets[index]++;

        long total = 0L;
        for (long count : minuteBuckets) {
            total += count;
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    long[] getMinuteBuckets() {
        return minuteBuckets.clone();
    }

    long getLastUpdatedMinute() {
        return lastUpdatedMinute;
    }

    private void advance(long currentMinute) {
        long elapsed = currentMinute - lastUpdatedMinute;
        if (elapsed < 0L || elapsed >= minuteBuckets.length) {
            Arrays.fill(minuteBuckets, 0L);
        } else {
            for (long minute = lastUpdatedMinute + 1L;
                minute <= currentMinute;
                minute++) {
                minuteBuckets[Math.floorMod(
                    minute,
                    minuteBuckets.length
                )] = 0L;
            }
        }
        lastUpdatedMinute = currentMinute;
    }

    private void resizeIfNeeded(int windowMinutes, long currentMinute) {
        if (minuteBuckets.length == windowMinutes) {
            return;
        }
        minuteBuckets = new long[windowMinutes];
        lastUpdatedMinute = currentMinute;
    }
}
