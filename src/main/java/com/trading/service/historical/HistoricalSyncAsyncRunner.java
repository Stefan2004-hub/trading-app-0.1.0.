package com.trading.service.historical;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class HistoricalSyncAsyncRunner {

    @Async("historicalSyncExecutor")
    public void runExtremeSync(Runnable task) {
        task.run();
    }
}
