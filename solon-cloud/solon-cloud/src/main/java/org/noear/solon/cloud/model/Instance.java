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
package org.noear.solon.cloud.model;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.utils.LocalUtils;
import org.noear.solon.core.Props;
import org.noear.solon.core.Signal;
import org.noear.solon.core.SignalSim;
import org.noear.solon.core.SignalType;
import org.noear.solon.core.util.KeyValues;

import java.io.Serializable;
import java.util.*;

/**
 * 服务节点模型
 *
 * @author noear
 * @since 1.2
 */
public class Instance implements Serializable {

    private String service;

    /**
     * 服务名；实例化后不能修改
     */
    public String service() {
        return service;
    }


    private String host;

    /**
     * 服务主机
     */
    public String host() {
        return host;
    }

    private int port;

    /**
     * 服务端口
     */
    public int port() {
        return port;
    }

    private String address;

    /**
     * 服务地址（host:port）
     * */
    public String address() {
        if (address == null) {
            address = host + ":" + port;
        }

        return address;
    }

    /**
     * 服务和地址(service@ip:port)
     */
    private String serviceAndAddress;

    public String serviceAndAddress() {
        if (serviceAndAddress == null) {
            serviceAndAddress = service() + "@" + address();
        }

        return serviceAndAddress;
    }

    private String protocol;

    /**
     * 协议（http, ws, tcp...）
     */
    public String protocol() {
        return protocol;
    }

    public Instance protocol(String protocol) {
        if (Utils.isNotEmpty(protocol)) {
            this.protocol = protocol;
            _uri = null;
        }

        return this;
    }

    private transient String _uri;

    public String uri() {
        if (_uri == null) {
            if (Utils.isEmpty(protocol)) {
                _uri = "http://" + address();
            } else {
                _uri = protocol + "://" + address();
            }
        }

        return _uri;
    }

    /**
     * 权重
     */
    private double weight = 1.0D;

    public double weight() {
        return weight;
    }

    public Instance weight(double weight) {
        this.weight = weight;
        return this;
    }

    /**
     * 元信息
     */
    private Map<String, String> meta = new HashMap<>();

    public Map<String, String> meta() {
        return meta;
    }

    public Instance metaPut(String name, String value) {
        if (value != null) {
            meta.put(name, value);
        }

        return this;
    }

    public String metaGet(String name) {
        return meta.get(name);
    }

    public Instance metaPutAll(Map<String, String> data) {
        if (data != null) {
            meta.putAll(data);

            protocol(meta.get("protocol"));
        }

        return this;
    }

    public Instance metaPutAll(Iterable<KeyValues<String>> data) {
        if (data != null) {
            for (KeyValues<String> kv : data) {
                meta.put(kv.getKey(), kv.getFirstValue());
            }

            protocol(meta.get("protocol"));
        }

        return this;
    }

    public Instance metaRemove(String name) {
        meta.remove(name);

        return this;
    }

    /**
     * 标签
     */
    private List<String> tags;

    public List<String> tags() {
        return tags;
    }

    public Instance tagsAdd(String tag) {
        if (tags == null) {
            tags = new ArrayList<>();
        }

        tags.add(tag);
        return this;
    }

    public Instance tagsAddAll(Collection<String> list) {
        if (tags == null) {
            tags = new ArrayList<>();
        }

        tags.addAll(list);
        return this;
    }


    /**
     * 用于序列化
     */
    public Instance() {

    }

    public Instance(String service, String host, int port) {
        if (Utils.isEmpty(service)) {
            service = Solon.cfg().appName();
        }

        if (port < 0) {
            port = 0;
        }

        this.service = service;
        this.host = host;
        this.port = port;
    }


    private static Instance local;

    public static Instance local() {
        if (local == null) {
            String _wrapHost = Solon.cfg().serverWrapHost(false);
            int _wrapPort = Solon.cfg().serverWrapPort(false);

            local = localNew(new SignalSim(Solon.cfg().appName(), _wrapHost, _wrapPort, "http", SignalType.HTTP));
        }

        return local;
    }

    public static Instance localNew(Signal signal) {

        Instance n1 = null;
        if (Utils.isEmpty(signal.host())) {
            n1 = new Instance(signal.name(), LocalUtils.getLocalAddress(), signal.port());
        } else {
            n1 = new Instance(signal.name(), signal.host(), signal.port());
        }

        n1.protocol(signal.protocol());

        //添加元信息
        n1.metaPutAll(getAppMeta());
        n1.metaPutAll(Solon.cfg().argx());
        n1.metaRemove("server.port"); //移除端口元信息
        n1.metaPut("protocol", signal.protocol());

        n1.tagsAdd("solon");
        if (Utils.isNotEmpty(Solon.cfg().appGroup())) {
            n1.tagsAdd(Solon.cfg().appGroup());
        }

        if (Utils.isNotEmpty(Solon.cfg().appName())) {
            n1.tagsAdd(Solon.cfg().appName());
        }
        n1.tagsAddAll(getAppTags());

        return n1;
    }

    private static Map<String, String> appMeta;

    /**
     * 获取应用元信息配置
     */
    private static Map<String, String> getAppMeta() {
        if (appMeta == null) {
            appMeta = new LinkedHashMap<>();

            Props metsProps = Solon.cfg().getProp("solon.app.meta");
            for (Map.Entry<Object, Object> kv : metsProps.entrySet()) {
                if (kv.getKey() instanceof String && kv.getValue() instanceof String) {
                    appMeta.put((String) kv.getKey(), (String) kv.getValue());
                }
            }
        }

        return appMeta;
    }

    private static List<String> appTags;

    /**
     * 获取应用标签
     */
    private static List<String> getAppTags() {
        if (appTags == null) {
            String tagsStr = Solon.cfg().get("solon.app.tags");

            if (Utils.isNotEmpty(tagsStr)) {
                appTags = Arrays.asList(tagsStr.split(","));
            } else {
                appTags = new ArrayList<>();
            }
        }

        return appTags;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "service='" + service + '\'' +
                ", protocol='" + protocol + '\'' +
                ", weight=" + weight +
                ", address='" + address() + '\'' +
                '}';
    }
}