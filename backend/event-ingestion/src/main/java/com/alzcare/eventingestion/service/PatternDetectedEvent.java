package com.alzcare.eventingestion.service;

import java.util.UUID;

/** Published inside the ingest transaction; handled after commit to send notifications. */
public record PatternDetectedEvent(UUID patternId) {
}
