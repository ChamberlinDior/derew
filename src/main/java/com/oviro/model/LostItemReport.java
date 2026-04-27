package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "lost_item_reports", indexes = {
    @Index(name = "idx_lost_ride", columnList = "ride_id"),
    @Index(name = "idx_lost_reporter", columnList = "reporter_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LostItemReport extends BaseEntity {

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(name = "item_color", length = 100)
    private String itemColor;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "resolved")
    @Builder.Default
    private boolean resolved = false;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;
}
