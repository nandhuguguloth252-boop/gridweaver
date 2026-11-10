package com.gridweaver.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "grid_events", indexes = {
        @Index(name = "idx_grid_event_time", columnList = "createdAt")
})
public class GridEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false) private String nodeId;
    @Column(nullable=false) private String zone;
    @Column(nullable=false) private String fromState;
    @Column(nullable=false) private String toState;
    @Column(nullable=false) private String reason;
    @Column(nullable=false) private Instant createdAt;
    private Double loadKw;
    private Double solarKw;

    public GridEventEntity() {}
    public GridEventEntity(String nodeId, String zone, String fromState, String toState,
                           String reason, Double loadKw, Double solarKw) {
        this.nodeId=nodeId; this.zone=zone; this.fromState=fromState; this.toState=toState;
        this.reason=reason; this.loadKw=loadKw; this.solarKw=solarKw; this.createdAt=Instant.now();
    }
    public Long getId(){return id;}
    public String getNodeId(){return nodeId;}
    public String getZone(){return zone;}
    public String getFromState(){return fromState;}
    public String getToState(){return toState;}
    public String getReason(){return reason;}
    public Instant getCreatedAt(){return createdAt;}
    public Double getLoadKw(){return loadKw;}
    public Double getSolarKw(){return solarKw;}
}
