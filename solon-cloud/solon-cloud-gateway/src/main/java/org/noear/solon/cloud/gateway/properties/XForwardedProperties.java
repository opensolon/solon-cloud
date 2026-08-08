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
package org.noear.solon.cloud.gateway.properties;

import java.io.Serializable;

/**
 * X-Forwarded-* 出站头生成配置，绑定 solon.cloud.gateway.xForwarded.*
 *
 * <ul>
 *   <li>enabled - 总开关，false 时不生成任何 X-Forwarded-* 头</li>
 *   <li>forEnabled / forAppend - X-Forwarded-For：append=true 逐跳追加对端 socket IP，false 覆盖</li>
 *   <li>hostEnabled / hostAppend - X-Forwarded-Host：默认覆盖式，值取原始 Host 头（对齐升级前行为）</li>
 *   <li>portEnabled / portAppend - X-Forwarded-Port：原始端口，缺省按 http80/https443</li>
 *   <li>protoEnabled / protoAppend - X-Forwarded-Proto：http/https</li>
 * </ul>
 *
 * <p>for 追加、host/port/proto 覆盖。</p>
 *
 * @author noear
 * @since 4.0.5
 */
public class XForwardedProperties implements Serializable {
    private boolean enabled = true;
    private boolean forEnabled = true;
    private boolean forAppend = true;
    private boolean hostEnabled = true;
    private boolean hostAppend = false;
    private boolean portEnabled = true;
    private boolean portAppend = false;
    private boolean protoEnabled = true;
    private boolean protoAppend = false;

    public XForwardedProperties() {

    }

    /**
     * 获取总开关
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取 X-Forwarded-For 生成开关
     */
    public boolean isForEnabled() {
        return forEnabled;
    }

    public void setForEnabled(boolean forEnabled) {
        this.forEnabled = forEnabled;
    }

    /**
     * 获取 X-Forwarded-For 追加开关（true 追加对端 IP，false 覆盖）
     */
    public boolean isForAppend() {
        return forAppend;
    }

    public void setForAppend(boolean forAppend) {
        this.forAppend = forAppend;
    }

    /**
     * 获取 X-Forwarded-Host 生成开关
     */
    public boolean isHostEnabled() {
        return hostEnabled;
    }

    public void setHostEnabled(boolean hostEnabled) {
        this.hostEnabled = hostEnabled;
    }

    /**
     * 获取 X-Forwarded-Host 追加开关
     */
    public boolean isHostAppend() {
        return hostAppend;
    }

    public void setHostAppend(boolean hostAppend) {
        this.hostAppend = hostAppend;
    }

    /**
     * 获取 X-Forwarded-Port 生成开关
     */
    public boolean isPortEnabled() {
        return portEnabled;
    }

    public void setPortEnabled(boolean portEnabled) {
        this.portEnabled = portEnabled;
    }

    /**
     * 获取 X-Forwarded-Port 追加开关
     */
    public boolean isPortAppend() {
        return portAppend;
    }

    public void setPortAppend(boolean portAppend) {
        this.portAppend = portAppend;
    }

    /**
     * 获取 X-Forwarded-Proto 生成开关
     */
    public boolean isProtoEnabled() {
        return protoEnabled;
    }

    public void setProtoEnabled(boolean protoEnabled) {
        this.protoEnabled = protoEnabled;
    }

    /**
     * 获取 X-Forwarded-Proto 追加开关
     */
    public boolean isProtoAppend() {
        return protoAppend;
    }

    public void setProtoAppend(boolean protoAppend) {
        this.protoAppend = protoAppend;
    }
}
