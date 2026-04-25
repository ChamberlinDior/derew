package com.oviro.enums;

public enum NotificationType {

    // Ride lifecycle
    RIDE_REQUEST,
    RIDE_ACCEPTED,
    RIDE_STARTED,
    RIDE_COMPLETED,
    RIDE_CANCELLED,
    DRIVER_ARRIVING,
    DRIVER_ARRIVED,

    // Payment & Wallet
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    WALLET_RECHARGED,
    WALLET_TRANSFER,

    // SOS
    SOS_ALERT,

    // Promotions & Marketing
    PROMO_OFFER,
    SUBSCRIPTION_EXPIRING,

    // Social
    RATING_REQUEST,
    NEW_MESSAGE,
    CHAT_MESSAGE,

    // Driver management
    DRIVER_APPROVED
}
