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
package org.noear.solon.cloud.extend.sentinel.impl;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.noear.solon.cloud.model.BreakerEntrySim;
import org.noear.solon.cloud.model.BreakerException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author noear
 * @since 1.3
 */
public class CloudBreakerEntryImpl extends BreakerEntrySim {
    private String breakerName;
    private int thresholdValue;

    public CloudBreakerEntryImpl(String breakerName, int permits) {
        this.breakerName = breakerName;
        this.thresholdValue = permits;

        loadRules();
    }

    private void loadRules(){
        List<FlowRule> ruleList = new ArrayList<>();
        FlowRule rule = null;

        rule = new FlowRule();
        rule.setResource(breakerName);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS); //qps
        rule.setCount(thresholdValue);
        ruleList.add(rule);

        rule = new FlowRule();
        rule.setResource(breakerName);
        rule.setGrade(RuleConstant.FLOW_GRADE_THREAD); //并发数
        rule.setCount(thresholdValue);
        ruleList.add(rule);

        FlowRuleManager.loadRules(ruleList);
    }

    @Override
    public AutoCloseable enter() throws BreakerException {
        try {
            return SphU.entry(breakerName, EntryType.IN);
        } catch (BlockException ex) {
            throw new BreakerException(ex);
        }
    }

    @Override
    public void reset(int value) {
        if(thresholdValue != value){
            thresholdValue = value;

            loadRules();
        }
    }
}
