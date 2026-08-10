package com.magu1436.craftbound.common.quality;

public record QualityState(int version, int quality) {
    public static final int CURRENT_VERSION = 1;
    public static final int MIN_QUALITY = 0;
    public static final int MAX_QUALITY = 100;

    public QualityState {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("unsupported quality state version: " + version);
        }
        if (!isValidQuality(quality)) {
            throw new IllegalArgumentException("quality is outside the valid range: " + quality);
        }
    }

    public static boolean isValidQuality(int quality) {
        return quality >= MIN_QUALITY && quality <= MAX_QUALITY;
    }
}
