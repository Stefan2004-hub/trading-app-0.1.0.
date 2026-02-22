package com.trading.service.historical;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.historical.sync")
public record HistoricalSyncProperties(
    int batchSize,
    long sleepMsBetweenBatches
) {

    public int batchSize() {
        return batchSize <= 0 ? 50 : batchSize;
    }

    public long sleepMsBetweenBatches() {
        return Math.max(0L, sleepMsBetweenBatches);
    }
}
