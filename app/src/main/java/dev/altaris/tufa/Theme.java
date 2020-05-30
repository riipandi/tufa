package dev.altaris.tufa;

public enum Theme {
    LIGHT,
    DARK,
    AMOLED,
    SYSTEM,
    SYSTEM_AMOLED;

    private static Theme[] _values;

    static {
        _values = values();
    }

    public static Theme fromInteger(int x) {
        return _values[x];
    }
}
