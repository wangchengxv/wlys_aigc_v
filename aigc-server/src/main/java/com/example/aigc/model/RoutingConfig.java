package com.example.aigc.model;

import com.example.aigc.repository.jpa.StringListJsonConverter;
import com.example.aigc.repository.jpa.TimeScheduleSlotListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routing_config")
public class RoutingConfig {

    @Id
    private Long id = 1L;
    private String strategy = "priority";
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "priority_connection_ids_json", columnDefinition = "LONGTEXT")
    private List<String> priorityConnectionIds = new ArrayList<>();
    private boolean failoverEnabled;
    private int failoverTimeoutSeconds = 10;
    @Convert(converter = TimeScheduleSlotListJsonConverter.class)
    @Column(name = "time_schedule_json", columnDefinition = "LONGTEXT")
    private List<TimeScheduleSlot> timeSchedule = new ArrayList<>();

    public RoutingConfig() {
    }

    public static RoutingConfig createDefault() {
        RoutingConfig config = new RoutingConfig();
        config.setId(1L);
        return config;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id == null ? 1L : id;
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
