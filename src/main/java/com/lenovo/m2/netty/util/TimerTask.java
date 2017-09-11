package com.lenovo.m2.netty.util;

import java.util.concurrent.TimeUnit;

/**
 * 定时任务
 *
 * @Author licy13
 * @Date 2017 /9/5
 */
public interface TimerTask {


    /**
     * 延时执行定时任务 {@link Timer#newTimeout(TimerTask, long, TimeUnit)}.
     *
     * @param timeout
     */
    void run(Timeout timeout) throws Exception;

}
