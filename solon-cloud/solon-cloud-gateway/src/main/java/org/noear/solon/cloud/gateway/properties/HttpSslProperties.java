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
 * Http 客户端 SSL 配置，绑定 solon.cloud.gateway.httpClient.ssl.*
 *
 * <ul>
 *   <li>enabled：总开关，默认 false（不配置即系统默认信任链）</li>
 *   <li>keyStore / keyStorePassword / keyStoreType：客户端证书库，keyStoreType 支持 JKS / PKCS12，
 *       缺省按 JKS 处理</li>
 *   <li>keyPassword：密钥口令，缺省回退 keyStorePassword</li>
 *   <li>trustStore / trustStorePassword / trustStoreType：自定义信任库（替换默认 CA 信任链）</li>
 * </ul>
 *
 * @author noear
 * @since 4.0.5
 */
public class HttpSslProperties implements Serializable {
    private boolean enabled = false;      //总开关
    private String keyStore;              //客户端证书库路径
    private String keyStorePassword;      //证书库口令
    private String keyStoreType;          //证书库类型：JKS / PKCS12（缺省 JKS）
    private String keyPassword;           //密钥口令（缺省回退 keyStorePassword）
    private String trustStore;            //自定义信任库路径
    private String trustStorePassword;    //信任库口令
    private String trustStoreType;        //信任库类型：JKS / PKCS12（缺省 JKS）

    public HttpSslProperties() {

    }

    /**
     * 是否启用 SSL 客户端配置
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取客户端证书库路径
     */
    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * 获取证书库口令
     */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    /**
     * 获取证书库类型（JKS / PKCS12）
     */
    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    /**
     * 获取密钥口令（缺省回退 keyStorePassword）
     */
    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    /**
     * 获取自定义信任库路径
     */
    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    /**
     * 获取信任库口令
     */
    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    /**
     * 获取信任库类型（JKS / PKCS12）
     */
    public String getTrustStoreType() {
        return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }
}
