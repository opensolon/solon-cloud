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
package org.noear.solon.cloud.extend.local.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.local.impl.CloudLocalUtils;
import org.noear.solon.cloud.model.Pack;
import org.noear.solon.cloud.service.CloudI18nService;
import org.noear.solon.core.Props;

/**
 * 云端国际化（本地摸拟实现）
 *
 * @author noear
 * @since 1.11
 */
public class CloudI18nServiceLocalImpl implements CloudI18nService {
    static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    static final String I18N_KEY_FORMAT = "i18n/%s_%s-%s";//{group}-{name}-{locale}
    static final String I18N_KEY_DEFAULT = "i18n/%s-%s";  //{name}-{language}
    private final String server;
    private String packNameDefault;

    public CloudI18nServiceLocalImpl(CloudProps cloudProps) {
        this.server = cloudProps.getServer();

        packNameDefault = cloudProps.getI18nDefault();
        //默认语言包名称,如果为空则设置为solon.app.name
        if (Utils.isEmpty(packNameDefault)) {
            packNameDefault = Solon.cfg().appName();
        }
        // 必须设置
        if (Utils.isEmpty(packNameDefault)) {
            //不能用日志服务（可能会死循环）
            System.err.println("[WARN] Solon.cloud no default i18n is configured");
        }
    }

    @Override
    public Pack pull(String group, String packName, Locale locale) {
        if(Utils.isEmpty(packName)){
            packName = packNameDefault;
        }

        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();

            if (Utils.isEmpty(group)) {
                group = DEFAULT_GROUP;
            }
        }
        
        Pack pack = new Pack(locale);
        pack.setData(new Props());
        
        List<String> i18nKeyList=new ArrayList<>();
        // 没有设置group时.读取的文件名中不包含组信息,使用具体的语言覆盖广泛的语言设置
        if(group.equals(DEFAULT_GROUP)) {
        	i18nKeyList.add(String.format(I18N_KEY_DEFAULT, packName, locale.getLanguage())); // zh
        	i18nKeyList.add(String.format(I18N_KEY_DEFAULT, packName, locale)); // zh_CN         	
        } else {
        	i18nKeyList.add(String.format(I18N_KEY_FORMAT, group, packName, locale.getLanguage()));
        	i18nKeyList.add(String.format(I18N_KEY_FORMAT, group, packName, locale));        	
        }
        Properties tmp;
        for (String i18nKey : i18nKeyList) {
        	tmp = getI18nProps(i18nKey);
        	if (tmp != null) {
                pack.getData().putAll(tmp);
            } 
		}
        return pack;
    }
    /**读取语言包的信息*/    
    private Properties getI18nProps(String i18nKey) {
        try {
            String value2 = CloudLocalUtils.getValue(server, i18nKey);

            if (Utils.isEmpty(value2)) {
                return null;
            } else {
                return Utils.buildProperties(value2);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
