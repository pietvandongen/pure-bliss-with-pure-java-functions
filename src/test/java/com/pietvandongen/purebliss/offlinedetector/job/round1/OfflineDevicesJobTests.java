package com.pietvandongen.purebliss.offlinedetector.job.round1;

import com.pietvandongen.purebliss.offlinedetector.domain.Device;
import com.pietvandongen.purebliss.offlinedetector.job.OfflineDevicesJob;
import com.pietvandongen.purebliss.offlinedetector.service.DeviceService;
import com.pietvandongen.purebliss.offlinedetector.service.PushNotificationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OfflineDevicesJobTests {

    @Mock
    private Clock clock;

    @Mock
    private DeviceService deviceService;

    @Mock
    private PushNotificationService pushNotificationService;

    private OfflineDevicesJob offlineDevicesJob;

    @Before
    public void setUp() {
        offlineDevicesJob = new OfflineDevicesJobImpl(clock, deviceService, pushNotificationService);
    }

    @Test
    public void thatNoPushNotificationIsSentWhenNoOfflineDevicesAreRegistered() {
        Duration threshold = Duration.ofDays(1);

        when(clock.instant()).thenReturn(Instant.now());

        offlineDevicesJob.onConfigurationUpdate(Collections.singletonList(threshold));
        offlineDevicesJob.run();

        verify(pushNotificationService, never()).sendOfflineNotification(any(Device.class));
    }

    @Test
    public void thatNoPushNotificationIsSentWhenNoThresholdWasPassed() {
        Duration threshold = Duration.ofDays(1);

        when(clock.instant()).thenReturn(Instant.now());

        offlineDevicesJob.onConfigurationUpdate(Collections.singletonList(threshold));
        offlineDevicesJob.onDeviceDisconnect(new Device(UUID.randomUUID()));
        offlineDevicesJob.run();

        verify(pushNotificationService, never()).sendOfflineNotification(any(Device.class));
    }

    @Test
    public void thatPushNotificationIsSentWhenSingleThresholdWasPassed() throws Exception {
        Duration threshold = Duration.ofDays(1);
        Instant disconnectInstant = Instant.parse("2010-10-10T10:10:00.00Z");
        Instant jobRunInstant = disconnectInstant.plus(threshold).plusSeconds(1);

        when(pushNotificationService.getLastOfflineNotificationInstant(any(Device.class))).thenReturn(Optional.empty());

        when(clock.instant()).thenReturn(disconnectInstant);

        offlineDevicesJob.onConfigurationUpdate(Collections.singletonList(threshold));
        offlineDevicesJob.onDeviceDisconnect(new Device(UUID.randomUUID()));

        when(clock.instant()).thenReturn(jobRunInstant);

        offlineDevicesJob.run();

        verify(pushNotificationService, times(1)).sendOfflineNotification(any(Device.class));
    }

    @Test
    public void thatPushNotificationIsSentWhenSingleThresholdWasPassedAndLastNotificationWasSentBeforeDeviceDisconnected() throws Exception {
        Duration threshold = Duration.ofDays(1);
        Instant disconnectInstant = Instant.parse("2010-10-10T10:10:00.00Z");
        Instant lastOfflineNotificationInstant = disconnectInstant.minus(1, ChronoUnit.DAYS);
        Instant jobRunInstant = disconnectInstant.plus(threshold).plusSeconds(1);

        when(pushNotificationService.getLastOfflineNotificationInstant(any(Device.class))).thenReturn(Optional.of(lastOfflineNotificationInstant));

        when(clock.instant()).thenReturn(disconnectInstant);

        offlineDevicesJob.onConfigurationUpdate(Collections.singletonList(threshold));
        offlineDevicesJob.onDeviceDisconnect(new Device(UUID.randomUUID()));

        when(clock.instant()).thenReturn(jobRunInstant);

        offlineDevicesJob.run();

        verify(pushNotificationService, times(1)).sendOfflineNotification(any(Device.class));
    }

    @Test
    public void thatPushNotificationIsNotSentWhenSingleThresholdWasPassedButNotificationWasAlreadySent() throws Exception {
        Duration threshold = Duration.ofDays(1);
        Instant disconnectInstant = Instant.parse("2010-10-10T10:10:00.00Z");
        Instant lastOfflineNotificationInstant = disconnectInstant.plus(threshold).plusSeconds(1);
        Instant jobRunInstant = disconnectInstant.plus(threshold).plusSeconds(2);

        when(pushNotificationService.getLastOfflineNotificationInstant(any(Device.class))).thenReturn(Optional.of(lastOfflineNotificationInstant));

        when(clock.instant()).thenReturn(disconnectInstant);

        offlineDevicesJob.onConfigurationUpdate(Collections.singletonList(threshold));
        offlineDevicesJob.onDeviceDisconnect(new Device(UUID.randomUUID()));

        when(clock.instant()).thenReturn(jobRunInstant);

        offlineDevicesJob.run();

        verify(pushNotificationService, never()).sendOfflineNotification(any(Device.class));
    }

    @Test
    public void thatPushNotificationIsSentWhenThresholdWasPassedAndNoNotificationWasEverSent() throws Exception {
        Duration firstThreshold = Duration.ofDays(1);
        Duration secondThreshold = Duration.ofDays(2);
        Instant disconnectInstant = Instant.parse("2010-10-10T10:10:00.00Z");
        Instant jobRunInstant = disconnectInstant.plus(firstThreshold).plusSeconds(1);

        when(pushNotificationService.getLastOfflineNotificationInstant(any(Device.class))).thenReturn(Optional.empty());

        when(clock.instant()).thenReturn(disconnectInstant);

        offlineDevicesJob.onConfigurationUpdate(Arrays.asList(firstThreshold, secondThreshold));
        offlineDevicesJob.onDeviceDisconnect(new Device(UUID.randomUUID()));

        when(clock.instant()).thenReturn(jobRunInstant);

        offlineDevicesJob.run();

        verify(pushNotificationService, times(1)).sendOfflineNotification(any(Device.class));
    }

    @Test
    public void thatPushNotificationIsNotSentWhenThresholdWasPassedButNotificationWasAlreadySent() throws Exception {
        Duration firstThreshold = Duration.ofDays(1);
        Duration secondThreshold = Duration.ofDays(2);
        Instant disconnectInstant = Instant.parse("2010-10-10T10:10:00.00Z");
        Instant lastOfflineNotificationInstant = disconnectInstant.plus(firstThreshold).plusSeconds(1);
        Instant jobRunInstant = disconnectInstant.plus(firstThreshold).plusSeconds(2);

        when(pushNotificationService.getLastOfflineNotificationInstant(any(Device.class))).thenReturn(Optional.of(lastOfflineNotificationInstant));

        when(clock.instant()).thenReturn(disconnectInstant);

        offlineDevicesJob.onConfigurationUpdate(Arrays.asList(firstThreshold, secondThreshold));
        offlineDevicesJob.onDeviceDisconnect(new Device(UUID.randomUUID()));

        when(clock.instant()).thenReturn(jobRunInstant);

        offlineDevicesJob.run();

        verify(pushNotificationService, never()).sendOfflineNotification(any(Device.class));
    }

    @Test
    public void thatPushNotificationIsSentWhenLastThresholdWasPassedAndNoNotificationWasEverSent() throws Exception {
        Duration firstThreshold = Duration.ofDays(1);
        Duration secondThreshold = Duration.ofDays(2);
        Instant disconnectInstant = Instant.parse("2010-10-10T10:10:00.00Z");
        Instant jobRunInstant = disconnectInstant.plus(secondThreshold).plusSeconds(1);

        when(pushNotificationService.getLastOfflineNotificationInstant(any(Device.class))).thenReturn(Optional.empty());

        when(clock.instant()).thenReturn(disconnectInstant);

        offlineDevicesJob.onConfigurationUpdate(Arrays.asList(firstThreshold, secondThreshold));
        offlineDevicesJob.onDeviceDisconnect(new Device(UUID.randomUUID()));

        when(clock.instant()).thenReturn(jobRunInstant);

        offlineDevicesJob.run();

        verify(pushNotificationService, times(1)).sendOfflineNotification(any(Device.class));
    }

    @Test
    public void thatPushNotificationIsSentWhenLastThresholdWasPassedAndLastSentNotificationWasForPreviousThreshold() throws Exception {
        Duration firstThreshold = Duration.ofDays(1);
        Duration secondThreshold = Duration.ofDays(2);
        Instant disconnectInstant = Instant.parse("2010-10-10T10:10:00.00Z");
        Instant lastOfflineNotificationInstant = disconnectInstant.plus(firstThreshold).plusSeconds(1);
        Instant jobRunInstant = disconnectInstant.plus(secondThreshold).plusSeconds(1);

        when(pushNotificationService.getLastOfflineNotificationInstant(any(Device.class))).thenReturn(Optional.of(lastOfflineNotificationInstant));

        when(clock.instant()).thenReturn(disconnectInstant);

        offlineDevicesJob.onConfigurationUpdate(Arrays.asList(firstThreshold, secondThreshold));
        offlineDevicesJob.onDeviceDisconnect(new Device(UUID.randomUUID()));

        when(clock.instant()).thenReturn(jobRunInstant);

        offlineDevicesJob.run();

        verify(pushNotificationService, times(1)).sendOfflineNotification(any(Device.class));
    }

    @Test
    public void thatPushNotificationIsNotSentWhenLastThresholdWasPassedButNotificationWasAlreadySent() throws Exception {
        Duration firstThreshold = Duration.ofDays(1);
        Duration secondThreshold = Duration.ofDays(2);
        Instant disconnectInstant = Instant.parse("2010-10-10T10:10:00.00Z");
        Instant lastOfflineNotificationInstant = disconnectInstant.plus(secondThreshold).plusSeconds(1);
        Instant jobRunInstant = disconnectInstant.plus(secondThreshold).plusSeconds(2);

        when(pushNotificationService.getLastOfflineNotificationInstant(any(Device.class))).thenReturn(Optional.of(lastOfflineNotificationInstant));

        when(clock.instant()).thenReturn(disconnectInstant);

        offlineDevicesJob.onConfigurationUpdate(Arrays.asList(firstThreshold, secondThreshold));
        offlineDevicesJob.onDeviceDisconnect(new Device(UUID.randomUUID()));

        when(clock.instant()).thenReturn(jobRunInstant);

        offlineDevicesJob.run();

        verify(pushNotificationService, never()).sendOfflineNotification(any(Device.class));
    }
}
