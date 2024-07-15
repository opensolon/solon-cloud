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
import org.noear.solon.core.util.RunUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 任务实体（内部使用）
 *
 * @author noear
 * @since 1.6
 */
class JobEntity extends Thread {
    static final Logger log = LoggerFactory.getLogger(JobEntity.class);

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
     * 是否取消任务
     */
    private boolean isCanceled;

    /**
     * 休息时间
     */
    private long sleepMillis;

    /**
     * 基准时间（对于比对）
     */
    private Date baseTime;
    /**
     * 下次执行时间
     */
    private Date nextTime;


    public JobEntity(String name, String description, long fixedRate, Runnable runnable) {
        this(name, description,null, fixedRate, runnable);
    }

    public JobEntity(String name, String description, CronExpressionPlus cron, Runnable runnable) {
        this(name,description, cron, 0,  runnable);
    }

    private JobEntity(String name,String description, CronExpressionPlus cron, long fixedRate, Runnable runnable) {
        this.cron = cron;
        this.description = description;
        this.fixedRate = fixedRate;
        this.runnable = runnable;

        this.baseTime = new Date();

        if (Utils.isNotEmpty(name)) {
            setName("Job:" + name);
        }
    }

    /**
     * 获取描述信息
     * @return
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
        this.baseTime = new Date(System.currentTimeMillis() + sleepMillis);
    }

    /**
     * 取消
     */
    public void cancel() {
        isCanceled = true;
    }

    /**
     * 运行
     */
    @Override
    public void run() {
        while (true) {
            if (isCanceled == false) {
                try {
                    scheduling();
                } catch (Throwable e) {
                    e = Utils.throwableUnwrap(e);
                    log.warn(e.getMessage(), e);
                }
            } else {
                break;
            }
        }
    }

    /**
     * 调度
     */
    private void scheduling() throws Throwable {
        if (fixedRate > 0) {
            //按固定频率调度
            sleepMillis = System.currentTimeMillis() - baseTime.getTime();

            if (sleepMillis >= fixedRate) {
                baseTime = new Date();
                execAsParallel();

                //重新设定休息时间
                sleepMillis = fixedRate;
            } else {
                //时间还未到（一般，第一次才会到这里来）
                sleepMillis = 100;
            }

            sleep0(sleepMillis);
        } else {
            //按表达式调度
            nextTime = cron.getNextValidTimeAfter(baseTime);
            sleepMillis = System.currentTimeMillis() - nextTime.getTime();

            if (sleepMillis >= 0) {
                baseTime = nextTime;
                nextTime = cron.getNextValidTimeAfter(baseTime);

                if (sleepMillis <= 1000) {
                    execAsParallel();

                    //重新设定休息时间
                    sleepMillis = System.currentTimeMillis() - nextTime.getTime();
                }
            }

            sleep0(sleepMillis);
        }
    }


    private void execAsParallel() {
        RunUtil.parallel(this::exec0);
    }

    private void exec0() {
        try {
            runnable.run();
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void sleep0(long sleep) {
        if (sleep < 0) {
            sleep = 100;
        }

        try {
            Thread.sleep(sleep);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }
}
