package com.ef.model;


public enum Variable {
    ACCESSLOG("accesslog"),
    STARTDATE("startDate"),
    DURATION("duration"),
    THRESHOLD("threshold");
    private final String name;

    Variable(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
