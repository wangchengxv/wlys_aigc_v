package com.example.aigc.model;

import java.util.ArrayList;
import java.util.List;

public class RoutingConfig {

    private String strategy = "priority";
    private List<String> priorityConnectionIds = new ArrayList<>();
    private boolean failoverEnabled;
    private int failoverTimeoutSeconds = 10;
    private List<TimeScheduleSlot> timeSchedule = new ArrayList<>();

    public RoutingConfig() {
    }

    public static RoutingConfig createDefault() {
        return new RoutingConfig();
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<String> getPriorityConnectionIds() {
        return priorityConnectionIds;
    }

    public void setPriorityConnectionIds(List<String> priorityConnectionIds) {
        this.priorityConnectionIds = priorityConnectionIds == null ? new ArrayList<>() : new ArrayList<>(priorityConnectionIds);
    }

    public boolean isFailoverEnabled() {
        return failoverEnabled;
    }

    public void setFailoverEnabled(boolean failoverEnabled) {
        this.failoverEnabled = failoverEnabled;
    }

    public int getFailoverTimeoutSeconds() {
        return failoverTimeoutSeconds;
    }

    public void setFailoverTimeoutSeconds(int failoverTimeoutSeconds) {
        this.failoverTimeoutSeconds = failoverTimeoutSeconds;
    }

    public List<TimeScheduleSlot> getTimeSchedule() {
        return timeSchedule;
    }

    public void setTimeSchedule(List<TimeScheduleSlot> timeSchedule) {
        this.timeSchedule = timeSchedule == null ? new ArrayList<>() : new ArrayList<>(timeSchedule);
    }
}
