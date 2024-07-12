package org.noear.solon.cloud.extend.consul.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.agent.model.Service;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudDiscoveryHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.model.Discovery;
import org.noear.solon.cloud.model.Instance;
import org.noear.solon.cloud.service.CloudDiscoveryObserverEntity;
import org.noear.solon.cloud.service.CloudDiscoveryService;
import org.noear.solon.cloud.utils.IntervalUtils;
import org.noear.solon.health.HealthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 云端注册与发现服务实现
 *
 * @author 夜の孤城
 * @author noear
 * @since 1.2
 */
public class CloudDiscoveryServiceConsulImpl extends TimerTask implements CloudDiscoveryService {
    static final Logger log = LoggerFactory.getLogger(CloudDiscoveryServiceConsulImpl.class);

    private ConsulClient client;
    private String token;

    private long refreshInterval;

    private String healthCheckInterval;

    private Map<String, Discovery> discoveryMap = new HashMap<>();
    private List<CloudDiscoveryObserverEntity> observerList = new ArrayList<>();


    public CloudDiscoveryServiceConsulImpl(CloudProps cloudProps) {
        token = cloudProps.getToken();
        refreshInterval = IntervalUtils.getInterval(cloudProps.getDiscoveryRefreshInterval("5s"));
        healthCheckInterval = cloudProps.getDiscoveryHealthCheckInterval("5s");

        String server = cloudProps.getDiscoveryServer();
        String[] ss = server.split(":");

        if (ss.length == 1) {
            client = new ConsulClient(ss[0]);
        } else {
            client = new ConsulClient(ss[0], Integer.parseInt(ss[1]));
        }
    }

    public long getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * 注册服务实例
     */
    @Override
    public void register(String group, Instance instance) {
        String[] ss = instance.address().split(":");
        String serviceId = instance.service() + "-" + instance.address();

        NewService newService = new NewService();

        newService.setId(serviceId);
        newService.setName(instance.service());
        newService.setAddress(ss[0]);
        newService.setPort(Integer.parseInt(ss[1]));
        newService.setMeta(instance.meta());

        if (instance.tags() != null) {
            newService.setTags(instance.tags());
        }

        registerLocalCheck(instance, newService);

        //
        // 注册服务
        //
        client.agentServiceRegister(newService, token);
    }

    @Override
    public void registerState(String group, Instance instance, boolean health) {
        String serviceId = instance.service() + "-" + instance.address();
        client.agentServiceSetMaintenance(serviceId, health);
    }

    private void registerLocalCheck(Instance instance, NewService newService) {
        if (Utils.isNotEmpty(healthCheckInterval)) {
            String protocol = Utils.annoAlias(instance.protocol(), "http");

            if (protocol.startsWith("http")) {
                String checkUrl = protocol + "://" + instance.address();
                if (HealthHandler.HANDLER_PATH.startsWith("/")) {
                    checkUrl = checkUrl + HealthHandler.HANDLER_PATH;
                } else {
                    checkUrl = checkUrl + "/" + HealthHandler.HANDLER_PATH;
                }

                NewService.Check check = new NewService.Check();
                check.setInterval(healthCheckInterval);
                check.setMethod("GET");
                check.setHttp(checkUrl);
                check.setDeregisterCriticalServiceAfter("30s");
                check.setTimeout("6s");

                newService.setCheck(check);
            }

            if (protocol.startsWith("tcp") || protocol.startsWith("ws")) {
                NewService.Check check = new NewService.Check();
                check.setInterval(healthCheckInterval);
                check.setTcp(instance.address());
                check.setTimeout("6s");

                newService.setCheck(check);
            }
        }
    }

    /**
     * 注销服务实例
     */
    @Override
    public void deregister(String group, Instance instance) {
        String serviceId = instance.service() + "-" + instance.address();
        client.agentServiceDeregister(serviceId);
    }

    /**
     * 查询服务实例列表
     */
    @Override
    public Discovery find(String group, String service) {
        return discoveryMap.get(service);
    }

    /**
     * 关注服务实例列表
     */
    @Override
    public void attention(String group, String service, CloudDiscoveryHandler observer) {
        observerList.add(new CloudDiscoveryObserverEntity(group, service, observer));
    }

    /**
     * 定时任务，刷新服务列表
     */
    @Override
    public void run() {
        try {
            run0();
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void run0() {
        Map<String, Discovery> discoveryTmp = new HashMap<>();
        Response<Map<String, Service>> services = client.getAgentServices();

        for (Map.Entry<String, Service> kv : services.getValue().entrySet()) {
            Service service = kv.getValue();

            if (Utils.isEmpty(service.getAddress())) {
                continue;
            }

            String name = service.getService();
            Discovery discovery = discoveryTmp.get(name);

            if (discovery == null) {
                discovery = new Discovery(service.getService());
                discoveryTmp.put(name, discovery);
            }

            Instance n1 = new Instance(service.getService(),
                    service.getAddress() + ":" + service.getPort())
                    .tagsAddAll(service.getTags())
                    .metaPutAll(service.getMeta());

            discovery.instanceAdd(n1);
        }

        discoveryMap = discoveryTmp;

        //通知观察者
        noticeObservers();
    }

    /**
     * 通知观察者
     */
    private void noticeObservers() {
        for (CloudDiscoveryObserverEntity entity : observerList) {
            Discovery tmp = discoveryMap.get(entity.service);
            if (tmp != null) {
                entity.handle(tmp);
            }
        }
    }
}
