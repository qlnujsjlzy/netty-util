package com.lenovo.m2.netty.util.test;

/**
 * @Author licy13
 * @Date 2017/9/4
 */

import com.lenovo.m2.netty.util.HashedWheelTimer;
import com.lenovo.m2.netty.util.Timeout;
import com.lenovo.m2.netty.util.Timer;
import com.lenovo.m2.netty.util.TimerTask;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class HashedWheelTimerTest {
    @Test
    public void testWorker() throws InterruptedException {
        final HashedWheelTimer timerProcessed = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 512);
        final AtomicInteger counter = new AtomicInteger();

        System.out.println("start:" + DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));

        timerProcessed.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                System.out.println("task1:" + DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()) + "==" + timeout);
            }
        }, 10, TimeUnit.MINUTES);

        Thread.sleep(1000);

        timerProcessed.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                System.out.println("task2:" + DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()) + "==" + timeout);
            }
        }, 20, TimeUnit.MINUTES);

        timerProcessed.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                System.out.println("task3:" + DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()) + "==" + timeout);
            }
        }, 30, TimeUnit.MINUTES);

        timerProcessed.openSwitch();

        Thread.sleep(3000);

        timerProcessed.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                System.out.println("task4:" + DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()) + "==" + timeout);
            }
        }, 4, TimeUnit.SECONDS);


        Thread.sleep(3000);

        timerProcessed.closeSwitch();

        timerProcessed.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                System.out.println("task5:" + DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()) + "==" + timeout);
            }
        }, 5, TimeUnit.SECONDS);

        Thread.sleep(10000);
    }

    @Test
    public void testScheduleTimeoutShouldNotRunBeforeDelay() throws InterruptedException {
        final Timer timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 512);
        final CountDownLatch barrier = new CountDownLatch(1);
        final Timeout timeout = timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                fail("This should not have run");
                barrier.countDown();
            }
        }, 10, TimeUnit.SECONDS);
        assertFalse(barrier.await(3, TimeUnit.SECONDS));
        assertFalse("timer should not expire", timeout.isExpired());
        timer.stop();
    }

    @Test
    public void testScheduleTimeoutShouldRunAfterDelay() throws InterruptedException {
        final Timer timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 512);
        ;
        final CountDownLatch barrier = new CountDownLatch(1);
        final Timeout timeout = timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                barrier.countDown();
            }
        }, 2, TimeUnit.SECONDS);
        assertTrue(barrier.await(3, TimeUnit.SECONDS));
        assertTrue("timer should expire", timeout.isExpired());
        timer.stop();
    }

    @Test
    public void testStopTimer() throws InterruptedException {
        final Timer timerProcessed = new HashedWheelTimer();
        ;
        for(int i = 0; i < 3; i++) {
            timerProcessed.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                }
            }, 1, TimeUnit.MILLISECONDS);
        }
        Thread.sleep(1000L); // sleep for a second
        assertEquals("Number of unprocessed timeouts should be 0", 0, timerProcessed.stop().size());

        final Timer timerUnprocessed =new HashedWheelTimer();
        ;
        for(int i = 0; i < 5; i++) {
            timerUnprocessed.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                }
            }, 5, TimeUnit.SECONDS);
        }
        Thread.sleep(1000L); // sleep for a second
        assertFalse("Number of unprocessed timeouts should be greater than 0", timerUnprocessed.stop().isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void testTimerShouldThrowExceptionAfterShutdownForNewTimeouts() throws InterruptedException {
        final Timer timer = new HashedWheelTimer();
        ;
        for(int i = 0; i < 3; i++) {
            timer.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                }
            }, 1, TimeUnit.MILLISECONDS);
        }

        timer.stop();
        Thread.sleep(1000L); // sleep for a second

        timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                fail("This should not run");
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Test
    public void testTimerOverflowWheelLength() throws InterruptedException {
        final HashedWheelTimer timer = new HashedWheelTimer(
                Executors.defaultThreadFactory(), 100, TimeUnit.MILLISECONDS, 32);
        final AtomicInteger counter = new AtomicInteger();

        timer.newTimeout(new TimerTask() {
            @Override
            public void run(final Timeout timeout) throws Exception {
                counter.incrementAndGet();
                timer.newTimeout(this, 1, TimeUnit.SECONDS);
            }
        }, 1, TimeUnit.SECONDS);
        Thread.sleep(3500);
        assertEquals(3, counter.get());
        timer.stop();
    }

    @Test
    public void testExecutionOnTime() throws InterruptedException {
        int tickDuration = 200;
        int timeout = 125;
        int maxTimeout = 2 * (tickDuration + timeout);
        final HashedWheelTimer timer = new HashedWheelTimer(tickDuration, TimeUnit.MILLISECONDS);
        final BlockingQueue<Long> queue = new LinkedBlockingQueue();

        int scheduledTasks = 100000;
        for(int i = 0; i < scheduledTasks; i++) {
            final long start = System.nanoTime();
            timer.newTimeout(new TimerTask() {
                @Override
                public void run(final Timeout timeout) throws Exception {
                    queue.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                }
            }, timeout, TimeUnit.MILLISECONDS);
        }

        for(int i = 0; i < scheduledTasks; i++) {
            long delay = queue.take();
            assertTrue("Timeout + " + scheduledTasks + " delay " + delay + " must be " + timeout + " < " + maxTimeout,
                    delay >= timeout && delay < maxTimeout);
            System.out.println("Timeout + " + scheduledTasks + " delay " + delay + " must be " + timeout + " < " + maxTimeout);

        }

        timer.stop();
    }
}
