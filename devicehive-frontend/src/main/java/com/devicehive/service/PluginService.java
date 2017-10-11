package com.devicehive.service;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.devicehive.dao.PluginDao;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.PluginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PluginService {
    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);
    
    private final HiveValidator hiveValidator;
    private final PluginDao pluginDao;

    @Autowired
    public PluginService(HiveValidator hiveValidator, PluginDao pluginDao) {
        this.hiveValidator = hiveValidator;
        this.pluginDao = pluginDao;
    }

    @Transactional
    public PluginVO register(PluginUpdate pluginUpdate) {
        PluginVO pluginVO = pluginUpdate.convertTo();
        if (pluginVO.getHealthCheckPeriod() == null) {
            pluginVO.setHealthCheckPeriod(300);
        }
        
        //TODO: topic should be created and topic name should be set
        String createdTopicName = "createdTopicName";
        pluginVO.setTopicName(createdTopicName);
        
        //TODO: subscription should be made
        
        pluginDao.persist(pluginVO);
        
        return pluginVO;
    }
}
