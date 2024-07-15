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
package org.noear.solon.cloud.extend.zookeeper.impl;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author noear
 * @since 1.3
 */
public class ZkClient {
    private CountDownLatch latch = new CountDownLatch(1);
    private String server;
    private int sessionTimeout;
    private ZooKeeper real;
    private final ReentrantLock SYNC_LOCK = new ReentrantLock();

    public ZkClient(String server, int sessionTimeout) {
        this.server = server;
        this.sessionTimeout = sessionTimeout;
    }

    public void connectServer() {
        SYNC_LOCK.lock();
        try {
            if (real != null) {
                return;
            }

            connectServer0();
        }finally {
            SYNC_LOCK.unlock();
        }
    }

    private void connectServer0() {
        try {
            real = new ZooKeeper(server, sessionTimeout, event -> {
                switch (event.getState()) {
                    case SyncConnected:
                        latch.countDown();
                        break;
                    case Expired:
                        connectServer0();
                        break;
                }
            });
            latch.await();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建节点
     */
    public void createNode(String path) {
        createNode(path, "", true);
    }

    public void createNode(String path, String data, boolean isPersistent) {
        try {
            if (real.exists(path, null) == null) {
                real.create(path, data.getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        (isPersistent ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 移徐节点
     */
    public void removeNode(String path) {
        try {
            real.delete(path, -1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置节点数据
     */
    public void setNodeData(String path, String data) {
        try {
            if (real.exists(path, false) == null) {
                createNode(path, data, true);
            } else {
                real.setData(path, data.getBytes(), -1);
            }
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取节点数据
     */
    public String getNodeData(String path) {
        try {
            byte[] bytes = real.getData(path, false, null);
            return new String(bytes);
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 监视节点数据
     */
    public void watchNodeData(String path, Watcher watcher) {
        try {
            real.getData(path, event -> {
                if (event.getType() == Watcher.Event.EventType.NodeCreated ||
                        event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    watcher.process(event);
                }
            }, null);
        } catch (KeeperException.NoNodeException e) {

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查找子节点
     */
    public List<String> findChildrenNode(String parentPath) {
        try {
            List<String> nodeList = real.getChildren(parentPath, null);

            List<String> nodeDataList = new ArrayList<>();

            for (String node : nodeList) {
                String nodeData = getNodeData(parentPath + "/" + node);
                nodeDataList.add(nodeData);
            }

            return nodeDataList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 监视子节点
     */
    public void watchChildrenNode(String parentPath, Watcher watcher) {
        try {
            real.getChildren(parentPath, event -> {
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    watcher.process(event);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 关闭
     */
    public void close() throws InterruptedException {
        if (real != null) {
            real.close();
        }
    }
}
