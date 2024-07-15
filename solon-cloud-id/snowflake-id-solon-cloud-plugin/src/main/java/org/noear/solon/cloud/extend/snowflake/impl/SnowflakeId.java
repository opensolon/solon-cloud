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
package org.noear.solon.cloud.extend.snowflake.impl;

import org.noear.solon.cloud.model.Instance;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author noear
 * @since 1.3
 */
public class SnowflakeId {
    //默认起始时间 2020-01-01 00:00:00（差不多可以用69年）
    private static final long START_TIME_DEF = 1577808000000L;

    public SnowflakeId(String dataBlock, long workId, long startTime) {
        if (startTime > 0) {
            this.startTime = startTime;
        } else {
            this.startTime = START_TIME_DEF;
        }

        //只能自动
        this.dataId = getDataId(dataBlock);

        //可以配置
        if (workId > 0) {
            this.workId = workId;
        } else {
            this.workId = getWorkId();
        }
    }

    public SnowflakeId(long dataId, long workId) {
        this(dataId, workId, START_TIME_DEF);
    }

    public SnowflakeId(long dataId, long workId, long startTime) {
        if (dataId > dataMaxNum || dataId < 0) {
            throw new IllegalArgumentException("dataId can't be greater than DATA_MAX_NUM or less than 0");
        }
        if (workId > workMaxNum || workId < 0) {
            throw new IllegalArgumentException("workId can't be greater than WORK_MAX_NUM or less than 0");
        }

        //默认起始时间 2020-01-01 00:00:00
        if (startTime > 0) {
            this.startTime = startTime;
        } else {
            this.startTime = START_TIME_DEF;
        }

        this.dataId = dataId;
        this.workId = workId;
    }

    ////////////////////////////

    //时间部分所占长度(用69年)
    private final int timeLen = 41;
    //数据中心id所占长度
    private final int dataLen = 5;
    //机器id所占长度
    private final int workLen = 5;
    //毫秒内序列所占长度
    private final int seqLen = 12;
    //定义起始时间
    private final long startTime;
    //上次生成ID的时间截
    private long lastTimeStamp = -1L;
    //时间部分向左移动的位数 22
    private final int timeLeftBit = 64 - 1 - timeLen;

    //自动获取数据中心id（可以手动定义 0-31之间的数）
    private final long dataId;
    //自动机器id（可以手动定义 0-31之间的数）
    private final long workId;
    //数据中心id最大值 31
    private final int dataMaxNum = ~(-1 << dataLen);
    //机器id最大值 31
    private final int workMaxNum = ~(-1 << workLen);
    //随机获取数据中心id的参数 32
    private final int dataRandom = dataMaxNum + 1;
    //随机获取机器id的参数 32
    private final int workRandom = workMaxNum + 1;
    //数据中心id左移位数 17
    private final int dataLeftBit = timeLeftBit - dataLen;
    //机器id左移位数 12
    private final int workLeftBit = dataLeftBit - workLen;

    //上一次的毫秒内序列值
    private long seqLastVal = 0L;
    //毫秒内序列的最大值 4095
    private final long seqMaxNum = ~(-1 << seqLen);

    private final ReentrantLock ID_LOCK = new ReentrantLock();


    /**
     * 获取下一个Id
     */
    public long nextId() {
        ID_LOCK.lock();

        try {
            return nextId0();
        } finally {
            ID_LOCK.unlock();
        }
    }

    private long nextId0() {
        long now = System.currentTimeMillis();

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (now < lastTimeStamp) {
            throw new IllegalStateException("System time error refused to generate snowflake ID!");
        }

        if (now == lastTimeStamp) {
            seqLastVal = (seqLastVal + 1) & seqMaxNum;
            if (seqLastVal == 0) {
                now = nextMillis(lastTimeStamp);
            }
        } else {
            seqLastVal = 0;
        }

        //上次生成ID的时间截
        lastTimeStamp = now;

        return ((now - startTime) << timeLeftBit) | (dataId << dataLeftBit) | (workId << workLeftBit) | seqLastVal;
    }


    /**
     * 获取下一不同毫秒的时间戳，不能与最后的时间戳一样
     */
    protected long nextMillis(long lastMillis) {
        long now = System.currentTimeMillis();
        while (now <= lastMillis) {
            now = System.currentTimeMillis();
        }
        return now;
    }

    //根据 host address 取余，发生异常就获取 0到31之间的随机数
    protected int getWorkId() {
        try {
            //ip:port
            return getHostId(Instance.local().address(), workMaxNum);
        } catch (Exception e) {
            return new Random().nextInt(workRandom);
        }
    }

    //根据 data block 取余，发生异常就获取 0到31之间的随机数
    protected int getDataId(String dataBlock) {
        try {
            return getHostId(dataBlock, dataMaxNum);
        } catch (Exception e) {
            return new Random().nextInt(dataRandom);
        }
    }

    //获取字符串s的字节数组，然后将数组的元素相加，对（max+1）取余
    private int getHostId(String s, int max) {
        byte[] bytes = s.getBytes();
        int sums = 0;
        for (int b : bytes) {
            sums += b;
        }
        return sums % (max + 1);
    }
}
