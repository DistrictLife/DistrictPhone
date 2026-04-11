package com.districtlife.phone.call;

public enum CallSignal {
    CALL(0),
    ACCEPT(1),
    DECLINE(2),
    HANGUP(3);

    private final int id;

    CallSignal(int id) { this.id = id; }

    public int getId() { return id; }

    public static CallSignal fromId(int id) {
        for (CallSignal s : values()) {
            if (s.id == id) return s;
        }
        return HANGUP;
    }
}
