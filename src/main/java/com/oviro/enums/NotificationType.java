package com.oviro.enums;

public enum NotificationType {

    // Ride lifecycle – passenger receives these
    RIDE_ACCEPTED,
    DRIVER_ARRIVED,
    RIDE_STARTED,
    RIDE_COMPLETED,

    // SOS – admins receive this
    SOS_ALERT,

    // Wallet – user who recharged receives this
    WALLET_RECHARGED
}
