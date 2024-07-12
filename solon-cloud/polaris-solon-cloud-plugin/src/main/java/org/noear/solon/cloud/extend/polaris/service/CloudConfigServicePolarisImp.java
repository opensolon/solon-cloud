package org.noear.solon.cloud.extend.polaris.service;

import com.tencent.polaris.configuration.api.core.*;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import com.tencent.polaris.factory.config.global.ClusterConfigImpl;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudConfigHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.exception.CloudConfigException;
import org.noear.solon.cloud.extend.polaris.PolarisProps;
import org.noear.solon.cloud.model.Config;
import org.noear.solon.cloud.service.CloudConfigObserverEntity;
import org.noear.solon.cloud.service.CloudConfigService;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * @author 何荣振
 * @since 1.11
 * */
public class CloudConfigServicePolarisImp implements CloudConfigService , Closeable {
    private final CloudProps cloudProps;
    private ConfigFileService real;


    public CloudConfigServicePolarisImp(CloudProps cloudProps) {
        this.cloudProps = cloudProps;
        String server = cloudProps.getConfigServer();
        String namespace = cloudProps.getNamespace();

        ConfigurationImpl cfgImpl = PolarisProps.getCfgImpl();

        //配置集群设置
        ClusterConfigImpl clusterConfig = cfgImpl.getGlobal().getSystem().getConfigCluster();
        clusterConfig.setNamespace(namespace);


        //配置连接设置(8093)
        ConnectorConfigImpl connectorConfig = cfgImpl.getConfigFile().getServerConnector();
        List<String> address = connectorConfig.getAddresses();
        address.add(server);
        connectorConfig.setAddresses(address);

        this.real = ConfigFileServiceFactory.createConfigFileService(cfgImpl);
    }

    /**
     * 拉取配置
     *
     * @param group 分组
     * @param name  配置名
     * @return
     */
    @Override
    public Config pull(String group, String name) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        ConfigFile configFile = real.getConfigFile(cloudProps.getNamespace(), group, name);
        return new Config(group, name, configFile.getContent(), 0);
    }

    /**
     * 设置配置
     *
     * @param group 分组
     * @param name  配置名
     * @param value 值
     * @return
     */
    @Override
    public boolean push(String group, String name, String value) {
        throw new CloudConfigException("Polaris does not support config push");
    }

    /**
     * @param group 分组
     * @param name  配置名
     * @return
     */
    @Override
    public boolean remove(String group, String name) {
        throw new CloudConfigException("Polaris does not support config remove");
    }

    /**
     * 监听配置的修改
     *
     * @param group    分组
     * @param name     配置名
     * @param observer 观察者
     */
    @Override
    public void attention(String group, String name, CloudConfigHandler observer) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        CloudConfigObserverEntity entity = new CloudConfigObserverEntity(group, name, observer);

        ConfigFile configFile = real.getConfigFile(cloudProps.getNamespace(), group, name);

        configFile.addChangeListener(event -> {
            entity.handle(new Config(entity.group, entity.key, event.getNewValue(), System.currentTimeMillis()));
        });
    }

    @Override
    public void close() throws IOException {

    }
}
