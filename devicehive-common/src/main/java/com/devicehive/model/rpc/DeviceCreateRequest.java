package com.devicehive.model.rpc;

/*
 * #%L
 * DeviceHive Common Module
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

import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;
import com.devicehive.vo.DeviceVO;


public class DeviceCreateRequest extends Body {

    private DeviceVO device;
    private Long oldNetwork;

    public DeviceCreateRequest(DeviceVO device, Long oldNetwork) {
        super(Action.DEVICE_CREATE_REQUEST);
        this.device = device;
        this.oldNetwork = oldNetwork;
    }

    public DeviceVO getDevice() {
        return device;
    }

    public void setDevice(DeviceVO device) {
        this.device = device;
    }

    public Long getOldNetwork() {
        return oldNetwork;
    }

    public void setOldNetwork(Long oldNetwork) {
        this.oldNetwork = oldNetwork;
    }

    @Override
    public String toString() {
        return "DeviceCreateRequest{" +
                "device=" + device +
                "oldNetwork=" + oldNetwork +
                '}';
    }
}
