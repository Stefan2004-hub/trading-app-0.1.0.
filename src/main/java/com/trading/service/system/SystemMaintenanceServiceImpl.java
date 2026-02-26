package com.trading.service.system;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SystemMaintenanceServiceImpl implements SystemMaintenanceService {

    private final AtomicBoolean keepAliveActive = new AtomicBoolean(true);

    @Override
    public boolean isKeepAliveActive() {
        return keepAliveActive.get();
    }

    @Override
    public boolean toggleKeepAlive() {
        while (true) {
            boolean current = keepAliveActive.get();
            boolean next = !current;
            if (keepAliveActive.compareAndSet(current, next)) {
                return next;
            }
        }
    }
}
