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
package features.gateway.funs;

import io.netty.handler.ipfilter.IpSubnetFilterRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.gateway.exchange.ExFilter;
import org.noear.solon.cloud.gateway.exchange.ExNewResponse;
import org.noear.solon.cloud.gateway.exchange.ExPredicate;
import org.noear.solon.cloud.gateway.route.RouteFactoryManager;
import org.noear.solon.cloud.gateway.route.predicate.IpMatcher;
import org.noear.solon.cloud.gateway.route.predicate.PathPredicateFactory;
import org.noear.solon.rx.Completable;
import org.noear.solon.test.SolonTest;

/**
 * 谓词/过滤器工厂的边界与防御分支测试（补齐 100% 覆盖率）
 *
 * @author noear
 * @since 4.0.5
 */
@SolonTest
public class FactoryEdgeTest {
    RouteFactoryManager routeFactoryManager = new RouteFactoryManager();

    private ExFilter getFilter(String prefix, String config) {
        return routeFactoryManager.getFilter(prefix, config);
    }

    private ExPredicate getPredicate(String prefix, String config) {
        return routeFactoryManager.getPredicate(prefix, config);
    }

    // ================= filter 构造防御分支 =================

    @Test
    public void addRequestHeaderFilter_edgeConfigs() {
        // blank config
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("AddRequestHeader", ""));
        // parts.length != 2
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("AddRequestHeader", "name"));
        // name empty
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("AddRequestHeader", ",v"));
        // value empty（中间空串：split 保留，value 为真空串）
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("AddRequestHeader", "n,,x"));
    }

    @Test
    public void addResponseHeaderFilter_edgeConfigs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("AddResponseHeader", ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("AddResponseHeader", "name"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("AddResponseHeader", ",v"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("AddResponseHeader", "n,,x"));
    }

    @Test
    public void prefixPathFilter_edgeConfigs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("PrefixPath", ""));
    }

    @Test
    public void redirectToFilter_edgeConfigs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("RedirectTo", ""));
        // parts.length < 2
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("RedirectTo", "302"));
    }

    @Test
    public void removeRequestHeaderFilter_edgeConfigs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("RemoveRequestHeader", ""));
    }

    @Test
    public void removeResponseHeaderFilter_edgeConfigs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("RemoveResponseHeader", ""));
    }

    @Test
    public void rewritePathFilter_edgeConfigs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("RewritePath", ""));
        // parts.length != 2
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("RewritePath", "abc"));
    }

    @Test
    public void stripPrefixFilter_edgeConfigs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> getFilter("StripPrefix", ""));
    }

    // ================= filter doOnError 回调分支 =================

    @Test
    public void addResponseHeaderFilter_doOnError() {
        ExFilter filter = getFilter("AddResponseHeader", "app.ver,1");
        Assertions.assertNotNull(filter);

        ExNewResponse newResponse = new ExNewResponse();
        try {
            filter.doFilter(new ExContextEmpty() {
                @Override
                public ExNewResponse newResponse() {
                    return newResponse;
                }
            }, ctx -> Completable.error(new RuntimeException("boom"))).subscribe();
        } catch (Exception ignore) {
            // 错误已由 doOnError 消费，此处吞掉仅防 subscribe() 无参默认抛出
        }

        // doOnError 分支也应写入响应头
        Assertions.assertEquals(1, newResponse.getHeaders().size());
        Assertions.assertEquals("1", newResponse.getHeaders().get("app.ver"));
    }

    @Test
    public void removeResponseHeaderFilter_doOnError() {
        ExFilter filter = getFilter("RemoveResponseHeader", "app.ver");
        Assertions.assertNotNull(filter);

        ExNewResponse newResponse = new ExNewResponse();
        newResponse.headerAdd("a", "1");
        newResponse.headerAdd("app.ver", "1");

        try {
            filter.doFilter(new ExContextEmpty() {
                @Override
                public ExNewResponse newResponse() {
                    return newResponse;
                }
            }, ctx -> Completable.error(new RuntimeException("boom"))).subscribe();
        } catch (Exception ignore) {
            // 同上
        }

        // doOnError 分支也应移除响应头
        Assertions.assertEquals(1, newResponse.getHeaders().size());
        Assertions.assertNull(newResponse.getHeaders().get("app.ver"));
    }

    // ================= predicate 构造防御分支 =================

    @Test
    public void cookiePredicate_edgeConfigs() {
        // blank config
        Assertions.assertThrows(IllegalArgumentException.class, () -> getPredicate("Cookie", ""));
    }

    @Test
    public void headerPredicate_regexEmpty() {
        // 有 name 但 regex 为空（逗号后空格：split 保留尾元素，走 regex 判空分支）
        Assertions.assertThrows(IllegalArgumentException.class, () -> getPredicate("Header", "name, "));
    }

    @Test
    public void queryPredicate_regexEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> getPredicate("Query", "name, "));
    }

    @Test
    public void queryPredicate_patternNullBranches() {
        // 无 pattern：参数不存在 -> false；参数存在 -> true
        ExPredicate predicate = getPredicate("Query", "token");
        Assertions.assertNotNull(predicate);

        Assertions.assertFalse(predicate.test(new ExContextEmpty() {
            @Override
            public String rawQueryParam(String key) {
                return null;
            }
        }));

        Assertions.assertTrue(predicate.test(new ExContextEmpty() {
            @Override
            public String rawQueryParam(String key) {
                return "abc";
            }
        }));
    }

    // ================= 其余缺口 =================

    @Test
    public void pathPredicate_depth() {
        // "/demo/**"：常量段 "demo" + 通配段 "**"，PathMatcher 深度按 2 计
        PathPredicateFactory.PathPredicate predicate =
                new PathPredicateFactory.PathPredicate("/demo/**");
        Assertions.assertEquals(2, predicate.depth());
    }

    @Test
    public void ipMatcher_errorBranches() {
        // buildRule：IPv4 字面量格式但非法值（256 段超限）-> UnknownHostException -> IllegalArgumentException
        // （纯数字+点格式走 JDK 内建解析，不会触发 DNS）
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> IpMatcher.buildRule("256.1.1.1", "Test"));

        // matches：字面量格式但无法解析的 IP -> catch -> false
        IpSubnetFilterRule rule = IpMatcher.buildRule("192.168.1.0/24", "Test");
        Assertions.assertFalse(IpMatcher.matches(rule, "999.999.999.999"));
    }

    @Test
    public void cookiePredicate_regexEmpty() {
        // parts.length > 1 且 regex 为空（trim 后）-> Cookie regex cannot be empty
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> getPredicate("Cookie", "name, "));
    }

    @Test
    public void remoteAddrPredicates_blankConfig() {
        // RemoteAddr / XForwardedRemoteAddr 空配置 -> blank throw
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> getPredicate("RemoteAddr", ""));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> getPredicate("XForwardedRemoteAddr", ""));
    }

    @Test
    public void prefixPathFilter_noLeadingSlash() {
        // 非 "/" 开头 -> 自动补 "/"（不抛）
        Assertions.assertNotNull(getFilter("PrefixPath", "api"));
    }

    @Test
    public void redirectToFilter_addQuery() {
        // 3 段配置 -> addQuery=true；doFilter 时拼接 rawQueryString
        ExFilter filter = new RouteFactoryManager().buildFilter("RedirectTo=301,/app,true");
        Assertions.assertNotNull(filter);

        ExNewResponse newResponse = new ExNewResponse();
        filter.doFilter(new ExContextEmpty() {
            @Override
            public ExNewResponse newResponse() {
                return newResponse;
            }

            @Override
            public String rawQueryString() {
                return "?a=1";
            }
        }, ctx -> Completable.complete()).subscribe();

        Assertions.assertEquals(301, newResponse.getStatus());
        Assertions.assertEquals("/app?a=1", newResponse.getHeaders().get("Location"));
    }

    @Test
    public void rewritePathFilter_noSlash() {
        // 2 段但 regex/replacement 不以 "/" 开头 -> throw
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> getFilter("RewritePath", "abc,def"));
    }

    @Test
    public void pathPredicate_depthMultiRules() {
        // 两条规则：第一条 depth=3（/demo/a/**），第二条更浅 depth=2（/demo/**）
        // 触发 depthMin > rule.depth() 的更新分支，取最浅深度
        PathPredicateFactory.PathPredicate predicate =
                new PathPredicateFactory.PathPredicate("/demo/a/**,/demo/**");
        Assertions.assertEquals(2, predicate.depth());
    }
}
