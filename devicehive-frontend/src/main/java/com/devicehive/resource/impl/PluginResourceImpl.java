package com.devicehive.resource.impl;

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


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.resource.PluginResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.PluginService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.PluginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_SUBMITTED;
import static javax.ws.rs.core.Response.Status.CREATED;

@Service
public class PluginResourceImpl implements PluginResource {

    private static final Logger logger = LoggerFactory.getLogger(UserResourceImpl.class);

    private final HiveValidator hiveValidator;
    private final PluginService pluginService;

    @Autowired
    public PluginResourceImpl(HiveValidator hiveValidator, PluginService pluginService) {
        this.hiveValidator = hiveValidator;
        this.pluginService = pluginService;
    }
    
    @Override
    public Response register(long timeout, String deviceIdsString, String networkIdsString, String pollType, String namesString, String timestamp, PluginUpdate pluginUpdate) {
        hiveValidator.validate(pluginUpdate);
        PluginVO pluginVO = pluginService.register(pluginUpdate);
        return ResponseFactory.response(CREATED, pluginVO, PLUGIN_SUBMITTED);
    }
}
