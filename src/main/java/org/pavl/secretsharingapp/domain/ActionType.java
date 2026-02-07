package org.pavl.secretsharingapp.domain;

public enum ActionType {
    SECRET_CREATED,
    SECRET_VIEWED,
    SECRET_BURNED,
    SECRET_REMOVED,
    SECRET_PURGED,
    SECRET_EXPIRED,
    API_KEY_CREATED,
    RATE_LIMIT_HIT,
    AUTH_FAILED
}
