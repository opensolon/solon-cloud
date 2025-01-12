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
package org.noear.solon.cloud.extend.local.service;

import org.noear.snack.ONode;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.local.impl.CloudLocalUtils;
import org.noear.solon.cloud.service.CloudListService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 云端名单（本地摸拟实现）
 *
 * @author noear
 * @since 1.11
 */
public class CloudListServiceLocalImpl implements CloudListService {
    static final String LIST_KEY_FORMAT = "list/%s-%s.json";
    private Map<String, List<String>> listMap = new HashMap<>();

    private final String server;

    private final ReentrantLock SYNC_LOCK = new ReentrantLock();

    public CloudListServiceLocalImpl(CloudProps cloudProps) {
        this.server = cloudProps.getServer();
    }

    @Override
    public boolean inList(String names, String type, String value) {
        for (String name : names.split(",")) {
            try {
                if (inListDo(name, type, value)) {
                    return true;
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        return false;
    }

    private boolean inListDo(String name, String type, String value) throws IOException {
        String listKey = String.format(LIST_KEY_FORMAT, name, type);
        List<String> listVal = listMap.get(listKey);

        if (listVal == null) {
            SYNC_LOCK.lock();

            try {
                listVal = listMap.get(listKey);

                if (listVal == null) {
                    String value2 = CloudLocalUtils.getValue(server, listKey);

                    if (value2 == null) {
                        listVal = new ArrayList<>();
                    } else {
                        listVal = new ArrayList<>();

                        ONode oNode = ONode.load(value2);
                        if (oNode.isArray()) {
                            for (ONode o1 : oNode.ary()) {
                                listVal.add(o1.getString());
                            }
                        }
                    }

                    listMap.put(listKey, listVal);
                }
            } finally {
                SYNC_LOCK.unlock();
            }
        }

        return listVal.contains(value);
    }
}