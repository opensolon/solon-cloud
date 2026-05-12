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
package org.noear.solon.cloud.gateway.route.handler;

import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.utils.CloudURI;
import org.noear.solon.cloud.gateway.route.RouteFactoryManager;
import org.noear.solon.cloud.gateway.route.RouteHandler;
import org.noear.solon.core.LoadBalance;
import org.noear.solon.core.exception.StatusException;
import org.noear.solon.rx.Completable;

import java.net.URI;

/**
 * Lb 路由处理器
 *
 * @author noear
 * @since 2.9
 */
public class LbRouteHandler implements RouteHandler {
    private final RouteFactoryManager routeManager;

    public LbRouteHandler(RouteFactoryManager routeManager) {
        this.routeManager = routeManager;
    }

    @Override
    public String[] schemas() {
        return new String[]{"lb"};
    }

    @Override
    public Completable handle(ExContext ctx) {
        //构建新的目标
        CloudURI lbUri = ctx.targetNew();

        if (lbUri.getHost() == null) {
            throw new StatusException("Invalid target service: host is null", 400);
        }

        String tmp = LoadBalance.get(lbUri.getHost()).getServer(lbUri.getPort());
        if (tmp == null) {
            throw new StatusException("The target service does not exist", 404);
        }

        //配置新目标
        final CloudURI targetUri;
        if (lbUri.getSchemes().length == 1) {
            targetUri = CloudURI.create(tmp);
        } else {
            targetUri = buildTargetUri(lbUri.getTargetUri(), tmp);
        }

        ctx.targetNew(targetUri);

        //重新查找处理器
        RouteHandler handler = routeManager.getHandler(targetUri.getRootScheme());

        if (handler == null) {
            throw new StatusException("The target handler does not exist", 404);
        }

        return handler.handle(ctx);
    }

    private CloudURI buildTargetUri(URI lbUri, String tmp) {
        String scheme = lbUri.getScheme();
        if (scheme == null) {
            scheme = "http";
        }

        String uriStr;
        int idx = tmp.indexOf("://");
        if (idx == -1) {
            uriStr = scheme + "://" + tmp;
        } else {
            uriStr = scheme + tmp.substring(idx);
        }

        try {
            return CloudURI.create(uriStr);
        } catch (IllegalArgumentException e) {
            throw new StatusException("Invalid target URI: " + uriStr, 400);
        }
    }
}