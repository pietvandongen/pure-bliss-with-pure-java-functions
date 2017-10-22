package com.pietvandongen.purebliss.offlinedetector.domain;

import java.util.UUID;

/**
 * Represents an IoT Device
 */
public class Device {

    private final UUID uuid;

    public Device(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Device that = (Device) other;

        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
