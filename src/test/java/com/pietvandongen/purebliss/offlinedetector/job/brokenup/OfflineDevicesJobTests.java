package com.pietvandongen.purebliss.offlinedetector.job.brokenup;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Enclosed.class)
public class OfflineDevicesJobTests {

    public static class OfflineDevicesJobImplTests {

        @Test(expected = IllegalArgumentException.class)
        public void thatCalculateLastPassedThresholdThrowsExceptionWhenCurrentIsBeforeStart() {
            Instant start = Instant.now();
            Instant current = start.minusNanos(1);
            List<Duration> thresholds = Collections.singletonList(Duration.ofDays(1));

            OfflineDevicesJobImpl.calculateLastPassedThreshold(start, current, thresholds);
        }

        @Test(expected = IllegalArgumentException.class)
        public void thatCalculateLastPassedThresholdThrowsExceptionWhenCurrentIsSameAsStart() {
            Instant start = Instant.now();
            List<Duration> thresholds = Collections.singletonList(Duration.ofDays(1));

            OfflineDevicesJobImpl.calculateLastPassedThreshold(start, start, thresholds);
        }

        @Test(expected = IllegalArgumentException.class)
        public void thatCalculateLastPassedThresholdThrowsExceptionWhenThresholdsAreEmpty() {
            Instant start = Instant.now();
            Instant current = start.plusNanos(1);
            List<Duration> thresholds = Collections.emptyList();

            OfflineDevicesJobImpl.calculateLastPassedThreshold(start, current, thresholds);
        }

        @Test
        public void thatShouldSendNotificationWithoutLastNotificationPresentReturnsFalseWhenNoThresholdHasBeenPassed() {
            Instant jobStart = Instant.parse("2010-10-10T10:10:00.001Z");
            Instant deviceOffline = Instant.parse("2010-10-10T10:10:00.000Z");
            List<Duration> thresholds = Arrays.asList(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3));

            boolean expectedResult = false;
            boolean actualResult = OfflineDevicesJobImpl.shouldSendNotification(jobStart, deviceOffline, thresholds);

            assertThat(actualResult, is(expectedResult));
        }

        @Test
        public void thatShouldSendNotificationWithoutLastNotificationPresentReturnsTrueWhenAThresholdHasBeenPassed() {
            Instant jobStart = Instant.parse("2010-10-10T10:10:01.001Z");
            Instant deviceOffline = Instant.parse("2010-10-10T10:10:00.000Z");
            List<Duration> thresholds = Arrays.asList(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3));

            boolean expectedResult = true;
            boolean actualResult = OfflineDevicesJobImpl.shouldSendNotification(jobStart, deviceOffline, thresholds);

            assertThat(actualResult, is(expectedResult));
        }
    }

    @RunWith(Parameterized.class)
    public static class OfflineDevicesJobCalculateLastPassedThresholdTests {
        @Parameters(name = "Description = {0}, start = {1}, current = {2}, thresholds = {3}, expected result = {4}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {
                            "Should return empty when no threshold has been passed yet",
                            Instant.parse("2010-10-10T10:10:00.000Z"),
                            Instant.parse("2010-10-10T10:10:00.001Z"),
                            Arrays.asList(Duration.ofDays(1)),
                            Optional.empty()
                    },
                    {
                            "Should return only threshold when only threshold has been passed",
                            Instant.parse("2010-10-10T10:10:00.000Z"),
                            Instant.parse("2010-10-10T10:10:01.001Z"),
                            Arrays.asList(Duration.ofSeconds(1)),
                            Optional.of(Duration.ofSeconds(1))
                    },
                    {
                            "Should return first threshold when first threshold has been passed",
                            Instant.parse("2010-10-10T10:10:00.000Z"),
                            Instant.parse("2010-10-10T10:10:01.001Z"),
                            Arrays.asList(Duration.ofSeconds(1), Duration.ofSeconds(2)),
                            Optional.of(Duration.ofSeconds(1))
                    },
                    {
                            "Should return middle threshold when middle threshold has been passed",
                            Instant.parse("2010-10-10T10:10:00.000Z"),
                            Instant.parse("2010-10-10T10:10:02.001Z"),
                            Arrays.asList(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)),
                            Optional.of(Duration.ofSeconds(2))
                    },
                    {
                            "Should return last threshold when first last has been passed",
                            Instant.parse("2010-10-10T10:10:00.000Z"),
                            Instant.parse("2010-10-10T10:10:03.001Z"),
                            Arrays.asList(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)),
                            Optional.of(Duration.ofSeconds(3))
                    }
            });
        }

        @Parameter
        public String description;

        @Parameter(1)
        public Instant start;

        @Parameter(2)
        public Instant current;

        @Parameter(3)
        public List<Duration> thresholds;

        @Parameter(4)
        public Optional<Duration> expectedResult;

        @Test
        public void test() {
            assertThat(OfflineDevicesJobImpl.calculateLastPassedThreshold(start, current, thresholds), is(expectedResult));
        }
    }

    @RunWith(Parameterized.class)
    public static class OfflineDevicesJobShouldSendNotificationTests {
        @Parameters(name = "Description = {0}, job start = {1}, device offline = {2}, last notification = {3}, thresholds = {4}, expected result = {5}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {
                            "Should return false when no threshold has been passed yet",
                            Instant.parse("2010-10-10T10:10:00.001Z"),
                            Instant.parse("2010-10-10T10:10:00.000Z"),
                            Instant.parse("2010-10-10T10:09:00.000Z"),
                            Arrays.asList(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)),
                            false
                    },
                    {
                            "Should return true when a threshold has been passed and notification was from before job start",
                            Instant.parse("2010-10-10T10:10:01.001Z"),
                            Instant.parse("2010-10-10T10:10:00.000Z"),
                            Instant.parse("2010-10-10T10:09:00.000Z"),
                            Arrays.asList(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)),
                            true
                    },
                    {
                            "Should return false when a threshold has been passed but notification for threshold was already sent",
                            Instant.parse("2010-10-10T10:10:01.002Z"),
                            Instant.parse("2010-10-10T10:10:00.000Z"),
                            Instant.parse("2010-10-10T10:10:01.001Z"),
                            Arrays.asList(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)),
                            false
                    },
                    {
                            "Should return true when a threshold has been passed and last sent notification was for previous threshold",
                            Instant.parse("2010-10-10T10:10:02.001Z"),
                            Instant.parse("2010-10-10T10:10:00.000Z"),
                            Instant.parse("2010-10-10T10:10:01.001Z"),
                            Arrays.asList(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)),
                            true
                    }
            });
        }

        @Parameter
        public String description;

        @Parameter(1)
        public Instant jobStart;

        @Parameter(2)
        public Instant deviceOffline;

        @Parameter(3)
        public Instant lastNotification;

        @Parameter(4)
        public List<Duration> thresholds;

        @Parameter(5)
        public boolean expectedResult;

        @Test
        public void test() {
            assertThat(OfflineDevicesJobImpl.shouldSendNotification(jobStart, deviceOffline, lastNotification, thresholds), is(expectedResult));
        }
    }
}
