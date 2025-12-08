package com.alphaskyport.logistics.model;

import com.alphaskyport.masterdata.model.Country;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_tracking_events")
@Data
@NoArgsConstructor
public class ShipmentTrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "event_status", nullable = false)
    private String eventStatus;

    @Column(name = "event_location")
    private String eventLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_country_id")
    private Country eventCountry;

    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "event_source")
    private String eventSource; // 'system', 'manual', 'api', 'gps'

    @Column(name = "external_event_id")
    private String externalEventId;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
