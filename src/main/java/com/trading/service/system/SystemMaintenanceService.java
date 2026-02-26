package com.trading.service.system;

public interface SystemMaintenanceService {

    boolean isKeepAliveActive();

    boolean toggleKeepAlive();
}
