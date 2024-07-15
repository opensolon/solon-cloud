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
package org.noear.solon.cloud.extend.quartz.service;

import org.noear.solon.Utils;

import java.time.ZoneId;

/**
 * @author noear
 * @since 2.0
 */
public class Cron7X {
    private String cron;
    private ZoneId zone;
    private Long interval;

    /**
     * cron 表达式
     */
    public String getCron() {
        return cron;
    }

    /**
     * 时区
     */
    public ZoneId getZone() {
        return zone;
    }

    /**
     * 毫秒数
     */
    public Long getInterval() {
        return interval;
    }

    public static Cron7X parse(String cron7x) throws IllegalArgumentException {
        if (Utils.isEmpty(cron7x)) {
            throw new IllegalArgumentException("The cron7x expression is empty");
        }

        Cron7X tmp = new Cron7X();

        if (cron7x.indexOf(" ") < 0) {
            if (cron7x.endsWith("ms")) {
                tmp.interval = Long.parseLong(cron7x.substring(0, cron7x.length() - 2));
            } else if (cron7x.endsWith("s")) {
                tmp.interval = Long.parseLong(cron7x.substring(0, cron7x.length() - 1)) * 1000;
            } else if (cron7x.endsWith("m")) {
                tmp.interval = Long.parseLong(cron7x.substring(0, cron7x.length() - 1)) * 1000 * 60;
            } else if (cron7x.endsWith("h")) {
                tmp.interval = Long.parseLong(cron7x.substring(0, cron7x.length() - 1)) * 1000 * 60 * 60;
            } else if (cron7x.endsWith("d")) {
                tmp.interval = Long.parseLong(cron7x.substring(0, cron7x.length() - 1)) * 1000 * 60 * 60 * 24;
            } else {
                throw new IllegalArgumentException("Unsupported cron7x expression: " + cron7x);
            }
        } else {
            int tzIdx = cron7x.indexOf("+");
            if (tzIdx < 0) {
                tzIdx = cron7x.indexOf("-");
            }

            if (tzIdx > 0) {
                String tz = cron7x.substring(tzIdx);
                tmp.zone = ZoneId.of(tz);
                tmp.cron = cron7x.substring(0, tzIdx - 1);
            } else {
                tmp.cron = cron7x;
            }
        }

        return tmp;
    }
}
