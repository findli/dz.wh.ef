package com.ef.model;

public enum Duration {
    HOURLY(3600), DAILY(3600 * 24);
    public int seconds;

    Duration(int seconds) {
        this.seconds = seconds;
    }
}

