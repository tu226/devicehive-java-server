package com.devicehive.eventbus;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class for handling all subscribe, unsubscribe and get subscribers tricky logic
 */
public class SubscriberRegistry {

    /**
     * Map for holding subscriptions for particular subscription request id (i.e. subscriber).
     * The KEY in this map is an id of subscriber (subscription request) and the VALUE is a set of subscriptions for this subscriber.
     *
     * This map keeps track of all subscriptions for single subscriber so that it is possible to remove all of them
     * from {@link SubscriberRegistry#subscriptions} map during {@link SubscriberRegistry#unregister(Subscriber)} call
     */
    private MultiMap<Long, Subscription> subscriberSubscriptions;
    private final String SUBSCRIBER_SUBSCRIPTIONS_MAP = "SUBSCRIBER-SUBSCRIPTIONS-MAP";

    /**
     * Map that contains an information about subscribers for single subscription.
     * The KEY is subscription (e.g. subscription on device notifications) and the VALUE is a set of all subscriber's ids.
     * This map is used for actual routing of messages through the event bus
     */
    private MultiMap<Subscription, Long> subscriptions;
    private final String SUBSCRIPTIONS_MAP = "SUBSCRIPTIONS-MAP";

    private Map<Long, Subscriber> subscribers;
    private final String SUBSCRIBERS_MAP = "SUBSCRIBERS-MAP";

    @Autowired
    public void getHazelcastMaps(HazelcastInstance hazelcastClient) {
        subscriberSubscriptions = hazelcastClient.getMultiMap(SUBSCRIBER_SUBSCRIPTIONS_MAP);
        subscriptions = hazelcastClient.getMultiMap(SUBSCRIPTIONS_MAP);
        subscribers = hazelcastClient.getMap(SUBSCRIBERS_MAP);
    }

    /**
     * Registers subscription and subscriber in registry maps.
     * Performs following steps:
     *  - if subscriber doesn't have any subscriptions in {@link SubscriberRegistry#subscriberSubscriptions} - creates an empty list for him;
     *  - adds subscription into subscriber's list in {@link SubscriberRegistry#subscriberSubscriptions};
     *  - if nobody is subscribed to this subscription in {@link SubscriberRegistry#subscriptions} - initializes the list;
     *  - adds subscriber to this subscription's list in {@link SubscriberRegistry#subscriptions}
     *
     * @param subscriber - subscriber
     * @param subscription - subscription to subscribe to
     */
    void register(Subscriber subscriber, Subscription subscription) {
        subscriberSubscriptions.put(subscriber.getId(), subscription);
        subscriptions.put(subscription, subscriber.getId());
        subscribers.put(subscriber.getId(), subscriber);
    }

    /**
     * Unregisters subscriber from registry maps:
     *  - gets all subscriber's subscriptions from {@link SubscriberRegistry#subscriberSubscriptions}
     *  - removes subscriber from each subscription's list in {@link SubscriberRegistry#subscriptions}
     *  - removes entry from {@link SubscriberRegistry#subscriberSubscriptions}
     *
     * @param subscriber - subscriber
     */
    void unregister(Subscriber subscriber) {
        Long id = subscriber.getId();
        Collection<Subscription> subs = subscriberSubscriptions.get(id);
        if (subs != null) {
            subs.forEach(s -> subscriptions.remove(s, id));
        }
        subscribers.remove(id);
        subscriberSubscriptions.remove(id);
    }

    void unregister(Subscription subscription) {
        Collection<Long> subIds = subscriptions.get(subscription);
        if (subIds != null) {
            subIds.forEach(subId -> subscriberSubscriptions.remove(subId, subscription));
        }
        subscriptions.remove(subscription);
    }

    /**
     * @param subscription - subscription
     * @return - list of subscribers for subscription
     */
    Collection<Subscriber> getSubscribers(Subscription subscription) {
        Assert.notNull(subscription);
        Collection<Long> subIds = subscriptions.get(subscription);
        if (subIds != null) {
            return subIds.stream().map(subscribers::get).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @param subscriptionId - subscriptionId
     * @return - subscriber for subscriptionId
     */
    Subscriber getSubscriber(Long subscriptionId) {
        return this.subscribers.get(subscriptionId);
    }

    Collection<Subscription> getSubscriptions(Subscriber subscriber) {
        Assert.notNull(subscriber);
        Collection<Subscription> subs = subscriberSubscriptions.get(subscriber.getId());
        return subs != null ? subs : Collections.emptyList();
    }

    Collection<Subscription> getAllSubscriptions() {
        return subscriptions.keySet();
    }
}
