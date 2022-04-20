package com.example.musicsearch;

public enum Entity {
    AREA("area"),
    ARTIST("artist"),
    EVENT("event"),
    GENRE("genre"),
    INSTRUMENT("instrument"),
    LABEL("label"),
    PLACE("place"),
    RECORDING("recording"),
    RELEASE("release"),
    RELEASEGROUP("release-group"),
    SERIES("series"),
    WORK("work"),
    URL("url");

    private final String name;

    Entity(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        return name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}
