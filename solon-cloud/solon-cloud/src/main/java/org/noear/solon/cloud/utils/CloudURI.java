/*
 * Copyright 2017-2026 noear.org and authors
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
package org.noear.solon.cloud.utils;

import org.noear.solon.core.util.Assert;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 云端地址
 *
 * @author noear
 * @since 3.10.5
 */
public class CloudURI {
    public static CloudURI create(String uriString) throws IllegalArgumentException {
        try {
            return new CloudURI(uriString);
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }

    private final String[] schemes;
    private final URI targetUri;
    private final String rawUriString; // 记录原始字符串，提升 toString 性能

    public CloudURI(String uriString) throws URISyntaxException {
        if (Assert.isEmpty(uriString)) {
            throw new URISyntaxException(String.valueOf(uriString), "URI is empty");
        }

        int schemeEnd = uriString.indexOf("://");
        if (schemeEnd <= 0) {
            throw new URISyntaxException(uriString, "Missing scheme or '://'");
        }

        this.rawUriString = uriString;
        String schemePart = uriString.substring(0, schemeEnd);
        this.schemes = schemePart.split(":");

        // 确保协议段不以冒号结尾导致 split 产生空项（如 "lb::ws://"）
        if (schemes.length == 0 || Assert.isEmpty(schemes[0])) {
            throw new URISyntaxException(uriString, "Invalid scheme part");
        }

        String lastProtocol = schemes[schemes.length - 1];
        String innerUriString = lastProtocol + "://" + uriString.substring(schemeEnd + 3);
        this.targetUri = new URI(innerUriString);
    }

    /**
     * 获取多级协议
     */
    public String[] getSchemes() {
        return schemes;
    }

    /**
     * 获取根协议 (例如 lb:ws://... 返回 lb)
     */
    public String getRootScheme() {
        return schemes[0];
    }

    /**
     * 获取目标地址
     */
    public URI getTargetUri() {
        return targetUri;
    }

    /**
     * 获取主机
     */
    public String getHost() {
        return targetUri.getHost();
    }

    /**
     * 获取端口
     */
    public int getPort() {
        return targetUri.getPort();
    }

    @Override
    public String toString() {
        return rawUriString;
    }

    @Override
    public int hashCode() {
        return rawUriString.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CloudURI that = (CloudURI) obj;
        return rawUriString.equals(that.rawUriString);
    }
}