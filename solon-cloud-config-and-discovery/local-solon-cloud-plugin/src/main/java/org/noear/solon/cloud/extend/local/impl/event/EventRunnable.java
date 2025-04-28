/*
 * Copyright 2017-2025 noear.org and authors
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
package org.noear.solon.cloud.extend.local.impl.event;

import org.noear.solon.cloud.extend.local.service.CloudEventServiceLocalImpl;
import org.noear.solon.cloud.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear
 * @since 1.12
 */
public class EventRunnable implements Runnable {
    static final Logger log = LoggerFactory.getLogger(EventRunnable.class);

    private CloudEventServiceLocalImpl eventService;
    private Event event;

    public EventRunnable(CloudEventServiceLocalImpl eventService, Event event) {
        this.eventService = eventService;
        this.event = event;
    }

    @Override
    public void run() {
        try {
            //派发
            eventService.distribute(event);
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        }
    }
}
