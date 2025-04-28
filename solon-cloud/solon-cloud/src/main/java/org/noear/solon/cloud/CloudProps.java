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
package org.noear.solon.cloud;

import java.util.Properties;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Props;

/**
 * 云服务属性模板
 *
 * @author noear
 * @since 1.2
 */
public class CloudProps {
    public static final String PREFIX_properties = "properties.";

    public static final String PREFIX_config = "config.";
    public static final String PREFIX_discovery = "discovery.";
    public static final String PREFIX_event = "event.";
    public static final String PREFIX_lock = "lock.";
    public static final String PREFIX_log = "log.";
    public static final String PREFIX_trace = "trace.";
    public static final String PREFIX_metric = "metric.";
    public static final String PREFIX_file = "file.";
    public static final String PREFIX_i18n = "i18n.";
    public static final String PREFIX_id = "id.";
    public static final String PREFIX_list = "list.";
    public static final String PREFIX_job = "job.";

    public static String LOG_DEFAULT_LOGGER;

    private String ROOT = "solon.cloud.@@.";

    private String SERVER = "solon.cloud.@@.server";
    private String TOKEN = "solon.cloud.@@.token";
    private String ALARM = "solon.cloud.@@.alarm";


    private String NAMESPACE = "solon.cloud.@@.namespace";

    private String USERNAME = "solon.cloud.@@.username";
    private String PASSWORD = "solon.cloud.@@.password";

    private String ACCESS_KEY = "solon.cloud.@@.accessKey";
    private String SECRET_KEY = "solon.cloud.@@.secretKey";

    //配置服务相关
    private String CONFIG_ENABLE = "solon.cloud.@@.config.enable";
    private String CONFIG_SERVER = "solon.cloud.@@.config.server";
    private String CONFIG_LOAD = "solon.cloud.@@.config.load";
    private String CONFIG_REFRESH_INTERVAL = "solon.cloud.@@.config.refreshInterval";

    //发现服务相关
    private String DISCOVERY_ENABLE = "solon.cloud.@@.discovery.enable";
    private String DISCOVERY_SERVER = "solon.cloud.@@.discovery.server";
    private String DISCOVERY_CLUSTER_NAME = "solon.cloud.@@.discovery.clusterName";
    private String DISCOVERY_HEALTH_CHECK_INTERVAL = "solon.cloud.@@.discovery.healthCheckInterval";
    private String DISCOVERY_REFRESH_INTERVAL = "solon.cloud.@@.discovery.refreshInterval";

    //事件总线服务相关
    private String EVENT_ENABLE = "solon.cloud.@@.event.enable";
    private String EVENT_SERVER = "solon.cloud.@@.event.server";
    private String EVENT_PREFETCH_COUNT = "solon.cloud.@@.event.prefetchCount";
    private String EVENT_PUBLISH_TIMEOUT = "solon.cloud.@@.event.publishTimeout";

    private String EVENT_CHANNEL = "solon.cloud.@@.event.channel"; //虚拟通道（在客户端，可以多通道路由）
    private String EVENT_BROKER = "solon.cloud.@@.event.broker"; //broker
    private String EVENT_GROUP = "solon.cloud.@@.event.group"; //虚拟分组 //默认分组配置（即给所有的发送和订阅加上分组）
    private String EVENT_CONSUMER = "solon.cloud.@@.event.consumer"; //配置组
    private String EVENT_PRODUCER = "solon.cloud.@@.event.producer"; //配置组
    private String EVENT_CLIENT = "solon.cloud.@@.event.client"; //配置组

    //@deprecated 2.2
    private String EVENT_USERNAME = "solon.cloud.@@.event.username";
    //@deprecated 2.2
    private String EVENT_PASSWORD = "solon.cloud.@@.event.password";
    //@deprecated 2.2
    private String EVENT_ACCESS_KEY = "solon.cloud.@@.event.accessKey";
    //@deprecated 2.2
    private String EVENT_SECRET_KEY = "solon.cloud.@@.event.secretKey";


    //锁服务相关
    private String LOCK_ENABLE = "solon.cloud.@@.lock.enable";
    private String LOCK_SERVER = "solon.cloud.@@.lock.server";

    //日志总线服务相关
    private String LOG_ENABLE = "solon.cloud.@@.log.enable";
    private String LOG_SERVER = "solon.cloud.@@.log.server";
    private String LOG_DEFAULT = "solon.cloud.@@.log.default";

    //链路跟踪服务相关
    private String TRACE_ENABLE = "solon.cloud.@@.trace.enable";
    private String TRACE_EXCLUDE = "solon.cloud.@@.trace.exclude";


    //度量服务相关
    private String METRIC_ENABLE = "solon.cloud.@@.metric.enable";

    //文件服务相关
    private String FILE_ENABLE = "solon.cloud.@@.file.enable";
    private String FILE_BUCKET = "solon.cloud.@@.file.bucket";
    private String FILE_ENDPOINT = "solon.cloud.@@.file.endpoint";
    private String FILE_REGION_ID = "solon.cloud.@@.file.regionId";

    //@deprecated 2.2
    private String FILE_USERNAME = "solon.cloud.@@.file.username";
    //@deprecated 2.2
    private String FILE_PASSWORD = "solon.cloud.@@.file.password";
    //@deprecated 2.2
    private String FILE_ACCESS_KEY = "solon.cloud.@@.file.accessKey";
    //@deprecated 2.2
    private String FILE_SECRET_KEY = "solon.cloud.@@.file.secretKey";

    //国际化服务相关
    private String I18N_ENABLE = "solon.cloud.@@.i18n.enable";
    private String I18N_DEFAULT = "solon.cloud.@@.i18n.default";

    //ID服务相关
    private String ID_ENABLE = "solon.cloud.@@.id.enable";
    private String ID_START = "solon.cloud.@@.id.start";

    //名单服务相关
    private String LIST_ENABLE = "solon.cloud.@@.list.enable";

    //任务服务相关
    private String JOB_ENABLE = "solon.cloud.@@.job.enable";
    private String JOB_SERVER = "solon.cloud.@@.job.server";

    private final AppContext appContext;

    public CloudProps(AppContext appContext, String frame) {
        this.appContext = appContext;

        ROOT = ROOT.replace("@@", frame);

        SERVER = SERVER.replace("@@", frame);
        TOKEN = TOKEN.replace("@@", frame);
        ALARM = ALARM.replace("@@", frame);

        NAMESPACE = NAMESPACE.replace("@@", frame);

        USERNAME = USERNAME.replace("@@", frame);
        PASSWORD = PASSWORD.replace("@@", frame);
        ACCESS_KEY = ACCESS_KEY.replace("@@", frame);
        SECRET_KEY = SECRET_KEY.replace("@@", frame);

        CONFIG_ENABLE = CONFIG_ENABLE.replace("@@", frame);
        CONFIG_SERVER = CONFIG_SERVER.replace("@@", frame);
        CONFIG_LOAD = CONFIG_LOAD.replace("@@", frame);
        CONFIG_REFRESH_INTERVAL = CONFIG_REFRESH_INTERVAL.replace("@@", frame);

        DISCOVERY_ENABLE = DISCOVERY_ENABLE.replace("@@", frame);
        DISCOVERY_SERVER = DISCOVERY_SERVER.replace("@@", frame);
        DISCOVERY_CLUSTER_NAME = DISCOVERY_CLUSTER_NAME.replace("@@", frame);
        //DISCOVERY_UNSTABLE = DISCOVERY_UNSTABLE.replace("@@", frame);
        //DISCOVERY_HEALTH_CHECK_PATH = DISCOVERY_HEALTH_CHECK_PATH.replace("@@", frame);
        DISCOVERY_HEALTH_CHECK_INTERVAL = DISCOVERY_HEALTH_CHECK_INTERVAL.replace("@@", frame);
        //DISCOVERY_HEALTH_DETECTOR = DISCOVERY_HEALTH_DETECTOR.replace("@@", frame);
        DISCOVERY_REFRESH_INTERVAL = DISCOVERY_REFRESH_INTERVAL.replace("@@", frame);

        EVENT_ENABLE = EVENT_ENABLE.replace("@@", frame);
        EVENT_SERVER = EVENT_SERVER.replace("@@", frame);
        EVENT_PREFETCH_COUNT = EVENT_PREFETCH_COUNT.replace("@@", frame);
        EVENT_PUBLISH_TIMEOUT = EVENT_PUBLISH_TIMEOUT.replace("@@", frame);
        EVENT_CHANNEL = EVENT_CHANNEL.replace("@@", frame);
        EVENT_BROKER = EVENT_BROKER.replace("@@", frame);
        EVENT_GROUP = EVENT_GROUP.replace("@@", frame);
        EVENT_CONSUMER = EVENT_CONSUMER.replace("@@", frame);
        EVENT_PRODUCER = EVENT_PRODUCER.replace("@@", frame);
        EVENT_CLIENT = EVENT_CLIENT.replace("@@", frame);

        //@deprecated 2.2
        EVENT_USERNAME = EVENT_USERNAME.replace("@@", frame);
        //@deprecated 2.2
        EVENT_PASSWORD = EVENT_PASSWORD.replace("@@", frame);
        //@deprecated 2.2
        EVENT_ACCESS_KEY = EVENT_ACCESS_KEY.replace("@@", frame);
        //@deprecated 2.2
        EVENT_SECRET_KEY = EVENT_SECRET_KEY.replace("@@", frame);

        LOCK_ENABLE = LOCK_ENABLE.replace("@@", frame);
        LOCK_SERVER = LOCK_SERVER.replace("@@", frame);

        LOG_ENABLE = LOG_ENABLE.replace("@@", frame);
        LOG_SERVER = LOG_SERVER.replace("@@", frame);
        LOG_DEFAULT = LOG_DEFAULT.replace("@@", frame);

        TRACE_ENABLE = TRACE_ENABLE.replace("@@", frame);
        TRACE_EXCLUDE = TRACE_EXCLUDE.replace("@@", frame);

        METRIC_ENABLE = METRIC_ENABLE.replace("@@", frame);

        FILE_ENABLE = FILE_ENABLE.replace("@@", frame);
        FILE_ENDPOINT = FILE_ENDPOINT.replace("@@", frame);
        FILE_REGION_ID = FILE_REGION_ID.replace("@@", frame);
        FILE_BUCKET = FILE_BUCKET.replace("@@", frame);

        //@deprecated 2.2
        FILE_USERNAME = FILE_USERNAME.replace("@@", frame);
        //@deprecated 2.2
        FILE_PASSWORD = FILE_PASSWORD.replace("@@", frame);
        //@deprecated 2.2
        FILE_ACCESS_KEY = FILE_ACCESS_KEY.replace("@@", frame);
        //@deprecated 2.2
        FILE_SECRET_KEY = FILE_SECRET_KEY.replace("@@", frame);

        I18N_ENABLE = I18N_ENABLE.replace("@@", frame);
        I18N_DEFAULT = I18N_DEFAULT.replace("@@", frame);

        ID_ENABLE = ID_ENABLE.replace("@@", frame);
        ID_START = ID_START.replace("@@", frame);

        LIST_ENABLE = LIST_ENABLE.replace("@@", frame);

        JOB_ENABLE = JOB_ENABLE.replace("@@", frame);
        JOB_SERVER = JOB_SERVER.replace("@@", frame);
    }


    //
    //公共
    //
    public String getServer() {
        return appContext.cfg().get(SERVER);
    }

    public String getToken() {
        return appContext.cfg().get(TOKEN);
    }


    public String getAlarm() {
        return appContext.cfg().get(ALARM);
    }

    private String namespace;

    public String getNamespace() {
        namespace = appContext.cfg().get(NAMESPACE);
        if (Utils.isEmpty(namespace)) {
            namespace = Solon.cfg().appNamespace();
        }

        if (namespace == null) {
            namespace = "";
        }

        return namespace;
    }

    private String username;

    public String getUsername() {
        if (username == null) {
            username = appContext.cfg().get(USERNAME);

            if (username == null) {
                username = appContext.cfg().get(ACCESS_KEY); //支持 USERNAME 与 ACCESS_KEY 互用
            }

            if (username == null) {
                username = "";
            }
        }

        return username;
    }

    private String password;

    public String getPassword() {
        if (password == null) {
            password = appContext.cfg().get(PASSWORD);

            if (password == null) {
                password = appContext.cfg().get(SECRET_KEY); //支持 PASSWORD 与 SECRET_KEY 互用
            }

            if (password == null) {
                password = "";
            }
        }

        return password;
    }

    public String getAccessKey() {
        return getUsername();
    }

    public String getSecretKey() {
        return getPassword();
    }

    //
    //配置
    //
    public boolean getConfigEnable() {
        return appContext.cfg().getBool(CONFIG_ENABLE, true);
    }

    public String getConfigServer() {
        String tmp = appContext.cfg().get(CONFIG_SERVER);
        if (Utils.isEmpty(tmp)) {
            return getServer();
        } else {
            return tmp;
        }
    }

    public String getConfigLoad() {
        return appContext.cfg().get(CONFIG_LOAD);
    }

    public String getConfigRefreshInterval(String def) {
        return appContext.cfg().get(CONFIG_REFRESH_INTERVAL, def);//def:10s
    }


    //
    //发现
    //
    public boolean getDiscoveryEnable() {
        return appContext.cfg().getBool(DISCOVERY_ENABLE, true);
    }

    public String getDiscoveryServer() {
        String tmp = appContext.cfg().get(DISCOVERY_SERVER);
        if (Utils.isEmpty(tmp)) {
            return getServer();
        } else {
            return tmp;
        }
    }


    @Deprecated
    public String getDiscoveryTags() {
        return "";
    }

    public String getDiscoveryClusterName() {
        return appContext.cfg().get(DISCOVERY_CLUSTER_NAME);
    }


    //    @Deprecated
//    public boolean getDiscoveryUnstable() {
//        return Solon.cfg().isDriftMode(); //appContext.cfg().getBool(DISCOVERY_UNSTABLE, false);
//    }

    public String getDiscoveryHealthCheckInterval(String def) {
        return appContext.cfg().get(DISCOVERY_HEALTH_CHECK_INTERVAL, def); //def:5s
    }

    public String getDiscoveryRefreshInterval(String def) {
        return appContext.cfg().get(DISCOVERY_REFRESH_INTERVAL, def);//def:5s
    }

    //
    //事件总线服务相关
    //
    public boolean getEventEnable() {
        return appContext.cfg().getBool(EVENT_ENABLE, true);
    }

    private String eventServer;

    public String getEventServer() {
        if (eventServer == null) {
            eventServer = appContext.cfg().get(EVENT_SERVER);

            if (eventServer == null) {
                eventServer = getServer();
            }
        }

        return eventServer;
    }

    public int getEventPrefetchCount() {
        return appContext.cfg().getInt(EVENT_PREFETCH_COUNT, 0);
    }

    public long getEventPublishTimeout() {
        return getEventPublishTimeout(3000L);
    }

    public long getEventPublishTimeout(long def) {
        return appContext.cfg().getLong(EVENT_PUBLISH_TIMEOUT, def);
    }

    public String getEventChannel() {
        return appContext.cfg().get(EVENT_CHANNEL, "");
    }

    public String getEventBroker() {
        return appContext.cfg().get(EVENT_BROKER, "");
    }

    public String getEventGroup() {
        return appContext.cfg().get(EVENT_GROUP, "");
    }

    public Properties getEventConsumerProps() {
        return appContext.cfg().getProp(EVENT_CONSUMER);
    }

    public Properties getEventProducerProps() {
        return appContext.cfg().getProp(EVENT_PRODUCER);
    }

    public Properties getEventClientProps() {
        return appContext.cfg().getProp(EVENT_CLIENT);
    }

    private String eventUsername;

    public String getEventUsername() {
        if (eventUsername == null) {
            eventUsername = appContext.cfg().get(EVENT_USERNAME);

            if (eventUsername == null) {
                eventUsername = appContext.cfg().get(EVENT_ACCESS_KEY);
            }

            if (eventUsername == null) {
                eventUsername = getUsername();
            }
        }

        return eventUsername;
    }

    private String eventPassword;

    public String getEventPassword() {
        if (eventPassword == null) {
            eventPassword = appContext.cfg().get(EVENT_PASSWORD);

            if (eventPassword == null) {
                eventPassword = appContext.cfg().get(EVENT_SECRET_KEY);
            }

            if (eventPassword == null) {
                eventPassword = getPassword();
            }
        }

        return eventPassword;
    }

    public String getEventAccessKey() {
        return getEventUsername();
    }

    public String getEventSecretKey() {
        return getEventPassword();
    }

    //
    //锁服务相关
    //
    public boolean getLockEnable() {
        return appContext.cfg().getBool(LOCK_ENABLE, true);
    }

    public String getLockServer() {
        String tmp = appContext.cfg().get(LOCK_SERVER);
        if (Utils.isEmpty(tmp)) {
            return getServer();
        } else {
            return tmp;
        }
    }


    //
    //日志总线服务相关
    //
    public boolean getLogEnable() {
        return appContext.cfg().getBool(LOG_ENABLE, true);
    }

    public String getLogServer() {
        String tmp = appContext.cfg().get(LOG_SERVER);
        if (Utils.isEmpty(tmp)) {
            return getServer();
        } else {
            return tmp;
        }
    }

    public String getLogDefault() {
        return appContext.cfg().get(LOG_DEFAULT);
    }


    //
    //链路跟踪服务相关
    //
    public boolean getTraceEnable() {
        return appContext.cfg().getBool(TRACE_ENABLE, true);
    }


    public String getTraceExclude() {
        return appContext.cfg().get(TRACE_EXCLUDE);
    }

    //
    //度量服务相关
    //
    public boolean getMetricEnable() {
        return appContext.cfg().getBool(METRIC_ENABLE, true);
    }


    //
    //文件服务相关
    //
    public boolean getFileEnable() {
        return appContext.cfg().getBool(FILE_ENABLE, true);
    }

    public String getFileEndpoint() {
        return appContext.cfg().get(FILE_ENDPOINT);
    }

    public String getFileRegionId() {
        return appContext.cfg().get(FILE_REGION_ID);
    }

    public String getFileBucket() {
        return appContext.cfg().get(FILE_BUCKET);
    }

    private String fileUsername;

    public String getFileUsername() {
        if (fileUsername == null) {
            fileUsername = appContext.cfg().get(FILE_USERNAME);

            if (fileUsername == null) {
                fileUsername = appContext.cfg().get(FILE_ACCESS_KEY);
            }

            if (fileUsername == null) {
                fileUsername = getUsername();
            }
        }

        return fileUsername;
    }

    private String filePassword;


    public String getFilePassword() {
        if (filePassword == null) {
            filePassword = appContext.cfg().get(FILE_PASSWORD);

            if (filePassword == null) {
                filePassword = appContext.cfg().get(FILE_SECRET_KEY);
            }

            if (filePassword == null) {
                filePassword = getPassword();
            }
        }

        return filePassword;
    }


    public String getFileAccessKey() {
        return getFileUsername();
    }


    public String getFileSecretKey() {
        return getFilePassword();
    }

    //
    //国际化服务相关
    //
    public boolean getI18nEnable() {
        return appContext.cfg().getBool(I18N_ENABLE, true);
    }

    public String getI18nDefault() {
        return appContext.cfg().get(I18N_DEFAULT);
    }

    //
    //ID服务相关
    //
    public boolean getIdEnable() {
        return appContext.cfg().getBool(ID_ENABLE, true);
    }

    public long getIdStart() {
        return appContext.cfg().getLong(ID_START, 0L);
    }

    //
    //LIST服务相关
    //
    public boolean getListEnable() {
        return appContext.cfg().getBool(LIST_ENABLE, true);
    }

    //
    //JOB服务相关
    //
    public boolean getJobEnable() {
        return appContext.cfg().getBool(JOB_ENABLE, true);
    }

    public String getJobServer() {
        String tmp = appContext.cfg().get(JOB_SERVER);
        if (Utils.isEmpty(tmp)) {
            return getServer();
        } else {
            return tmp;
        }
    }

    /**
     * 获取值
     */
    public String getValue(String name) {
        return appContext.cfg().get(ROOT + name); //"solon.cloud.@@.";
    }

    public String getValue(String name, String def) {
        return appContext.cfg().get(ROOT + name, def); //"solon.cloud.@@.";
    }


    /**
     * 设置值
     */
    public void setValue(String name, String value) {
        appContext.cfg().setProperty(ROOT + name, value); //"solon.cloud.@@.";
    }

    /**
     * 获取所有属性
     */
    public Props getProp() {
        return appContext.cfg().getProp(ROOT);
    }

    /**
     * 获取所有某一块属性
     */
    public Props getProp(String keyStarts) {
        return appContext.cfg().getProp(ROOT + keyStarts);
    }
}