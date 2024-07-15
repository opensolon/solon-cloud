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
package org.noear.solon.cloud.extend.water.integration.msg;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.extend.water.service.CloudDiscoveryServiceWaterImpl;
import org.noear.solon.cloud.extend.water.service.CloudI18nServiceWaterImpl;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.logging.utils.TagsMDC;
import org.noear.water.WW;
import org.noear.wood.WoodConfig;
import org.noear.wood.cache.ICacheServiceEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear
 * @since 1.2
 */
public class HandlerCacheUpdate implements CloudEventHandler {
    static Logger logger = LoggerFactory.getLogger(WW.logger_water_log_upstream);

    CloudDiscoveryServiceWaterImpl discoveryService;
    CloudI18nServiceWaterImpl i18nService;

    public HandlerCacheUpdate(CloudDiscoveryServiceWaterImpl discoveryService, CloudI18nServiceWaterImpl i18nService){
        this.discoveryService = discoveryService;
        this.i18nService = i18nService;
    }

    @Override
    public boolean handle(Event event) {
        String[] tagKeyAry = event.content().split(";");

        for (String tagKey : tagKeyAry) {
            if (Utils.isNotEmpty(tagKey)) {
                this.cacheUpdateHandler0(tagKey);
                this.cacheUpdateHandler1(tagKey);
            }
        }

        return true;
    }

    /**
     * 更新 upstream
     * */
    public boolean cacheUpdateHandler0(String tagKey) {
        String[] ss = null;
        if (tagKey.contains("::")) {
            ss = tagKey.split("::");
        } else {
            ss = tagKey.split(":");
        }

        if (discoveryService != null) {
            if ("upstream".equals(ss[0])) {
                String service = ss[1];
                try {
                    discoveryService.onUpdate("", service);
                } catch (Exception ex) {
                    TagsMDC.tag0(ss[1]);
                    TagsMDC.tag1("reload");

                    logger.error("{}", ex);
                }

                return true;
            }
        }

        if (i18nService != null) {
            if ("i18n".equals(ss[0]) && ss.length >= 4) {
                i18nService.onUpdate(ss[1], ss[2], ss[3]);

                return true;
            }
        }

        return false;
    }

    /**
     * 更新 cache
     * */
    public void cacheUpdateHandler1(String tag) {
        if (tag.indexOf(".") > 0) {
            String[] ss = tag.split("\\.");
            if (ss.length == 2) {
                ICacheServiceEx cache = WoodConfig.libOfCache.get(ss[0]);
                if (cache != null) {
                    cache.clear(ss[1]);
                }
            }
        } else {
            for (ICacheServiceEx cache : WoodConfig.libOfCache.values()) {
                cache.clear(tag);
            }
        }
    }
}
