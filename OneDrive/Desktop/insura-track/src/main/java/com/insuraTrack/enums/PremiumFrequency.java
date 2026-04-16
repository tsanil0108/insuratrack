package com.insuraTrack.enums;

public enum PremiumFrequency {
    MONTHLY,
    QUARTERLY,
    HALF_YEARLY,
    ANNUALLY,
    YEARLY  // kept for backward compatibility if any existing DB data uses it
}