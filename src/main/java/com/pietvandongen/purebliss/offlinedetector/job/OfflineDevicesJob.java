package com.pietvandongen.purebliss.offlinedetector.job;

import com.pietvandongen.purebliss.offlinedetector.domain.Device;

import java.time.Duration;
import java.util.List;

public interface OfflineDevicesJob {

    /**
     * Is called periodically by a job scheduler.
     */
    void run();

    /**
     * Is called when a device connects.
     *
     * @param device The connecting device.
     */
    void onDeviceConnect(Device device);

    /**
     * Is called when a device disconnects.
     *
     * @param device The disconnecting device.
     */
    void onDeviceDisconnect(Device device);

    /**
     * Is called when the job configuration has been changed.
     *
     * @param thresholds A list of thresholds that determine when to send notifications.
     */
    void onConfigurationUpdate(List<Duration> thresholds);
}
