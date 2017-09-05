package com.devicehive.model.rpc;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 DataArt
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

import java.util.Objects;
import java.util.Set;

public class NotificationUnsubscribeRequest extends Body {

    private Long subscriptionId;
    private Set<String> deviceIds;

    public NotificationUnsubscribeRequest(Long subscriptionId, Set<String> deviceIds) {
        super(Action.NOTIFICATION_UNSUBSCRIBE_REQUEST);
        this.subscriptionId = subscriptionId;
        this.deviceIds = deviceIds;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Set<String> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(Set<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationUnsubscribeRequest)) return false;
        if (!super.equals(o)) return false;

        NotificationUnsubscribeRequest that = (NotificationUnsubscribeRequest) o;
        return Objects.equals(subscriptionId, that.subscriptionId)
                && Objects.equals(deviceIds, that.deviceIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subscriptionId, deviceIds);
    }

    @Override
    public String toString() {
        return "NotificationUnsubscribeRequest{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", deviceIds=" + deviceIds +
                '}';
    }
}
