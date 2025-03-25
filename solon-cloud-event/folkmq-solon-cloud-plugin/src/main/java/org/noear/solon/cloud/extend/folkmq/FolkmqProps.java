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
package org.noear.solon.cloud.extend.folkmq;

import org.noear.solon.Utils;
import org.noear.solon.cloud.model.Event;

/**
 * @author noear
 * @since 2.0
 */
public class FolkmqProps {
    public static final String CREATED_TIMESTAMP ="__CREATED_TIMESTAMP";

    public static final String GROUP_SPLIT_MARK = "--";

    public static String getTopicNew(Event event){
        //new topic
        String topicNew;
        if (Utils.isEmpty(event.group())) {
            topicNew = event.topic();
        } else {
            topicNew = event.group() + FolkmqProps.GROUP_SPLIT_MARK + event.topic();
        }

        return topicNew;
    }
}
