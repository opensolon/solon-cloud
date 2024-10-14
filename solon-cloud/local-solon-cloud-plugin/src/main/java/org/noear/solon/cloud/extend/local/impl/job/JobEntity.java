/*
 * Copyright 2017-2024 noear.org and authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.noear.solon.cloud.extend.local.impl.job;

import org.noear.java_cron.CronExpressionPlus;
import org.noear.solon.Utils;
import org.noear.solon.core.Lifecycle;
import org.noear.solon.core.util.RunUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 * 任务实体（内部使用）
 *
 * @author noear
 * @since 1.6
 */
class JobEntity implements Lifecycle {
    static final Logger log = LoggerFactory.getLogger(JobEntity.class);

    /**
     * 名字
     */
    private String name;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 调度表达式
     */
    private CronExpressionPlus cron;
    /**
     * 固定频率
     */
    private long fixedRate;
    /**
     * 执行函数
     */
    final Runnable runnable;

    /**
     * 延后时间
     */
    private long delayMillis;
    /**
     * 下次执行时间
     */
    private Date nextTime;
    /**
     * 执行任务
     */
    private ScheduledFuture<?> jobFutureOfFixed;
    private Future<?> jobFutureOfCron;


    public JobEntity(String name, String description, long fixedRate, Runnable runnable) {
        this(name, description, null, fixedRate, runnable);
    }

    public JobEntity(String name, String description, CronExpressionPlus cron, Runnable runnable) {
        this(name, description, cron, 0, runnable);
    }

    private JobEntity(String name, String description, CronExpressionPlus cron, long fixedRate, Runnable runnable) {
        this.cron = cron;
        this.name = name;
        this.description = description;
        this.fixedRate = fixedRate;
        this.runnable = runnable;
    }

    /**
     * 名字
     */
    public String getName() {
        return name;
    }

    /**
     * 描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * 重置调度时间
     */
    protected void reset(CronExpressionPlus cron, long fixedRate) {
        this.cron = cron;
        this.fixedRate = fixedRate;
    }


    /**
     * 是否开始
     */
    private boolean isStarted = false;

    /**
     * 是否已开始
     */
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * 开始
     */
    @Override
    public void start() {
        if (isStarted) {
            return;
        } else {
            isStarted = true;
        }

        //重置（可能会二次启动）
        nextTime = null;

        if (fixedRate > 0) {
            jobFutureOfFixed = RunUtil.scheduleAtFixedRate(this::exec0, 0L, fixedRate);
        } else {
            RunUtil.parallel(this::run);
        }
    }

    /**
     * 取消
     */
    @Override
    public void stop() {
        if (isStarted = false) {
            return;
        } else {
            isStarted = false;
        }

        if (jobFutureOfFixed != null) {
            jobFutureOfFixed.cancel(false);
        }

        if (jobFutureOfCron != null) {
            jobFutureOfCron.cancel(false);
        }
    }

    private void run() {
        if (isStarted == false) {
            return;
        }

        try {
            runAsCron();
        } catch (Throwable e) {
            e = Utils.throwableUnwrap(e);
            if (e instanceof InterruptedException) {
                //任务中止
                isStarted = false;
                return;
            }

            log.warn(e.getMessage(), e);
        }

        if (delayMillis < 0L) {
            delayMillis = 100L;
        }

        RunUtil.delay(this::run, delayMillis);
    }

    /**
     * 调度
     */
    private void runAsCron() throws Throwable {
        if (nextTime == null) {
            //说明是第一次
            nextTime = cron.getNextValidTimeAfter(new Date());
        }

        if (nextTime != null) {
            delayMillis = nextTime.getTime() - System.currentTimeMillis();

            if (delayMillis <= 0L) {
                //到时（=0）或超时（<0）了
                jobFutureOfCron = RunUtil.parallel(this::exec0);

                nextTime = cron.getNextValidTimeAfter(nextTime);
                if (nextTime != null) {
                    //重新设定休息时间
                    delayMillis = nextTime.getTime() - System.currentTimeMillis();
                }
            }
        }

        if (nextTime == null) {
            isStarted = false;
            log.warn("The cron expression has expired, and the job is complete!");
        }
    }

    private void exec0() {
        try {
            runnable.run();
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        }
    }
}