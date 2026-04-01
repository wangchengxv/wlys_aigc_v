package com.example.aigc.model;

public class TimeScheduleSlot {

    private String start;
    private String end;
    private String connectionId;

    public TimeScheduleSlot() {
    }

    public TimeScheduleSlot(String start, String end, String connectionId) {
        this.start = start;
        this.end = end;
        this.connectionId = connectionId;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
}
