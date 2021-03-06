package com.devicehive.proxy.client;

/*
 * #%L
 * DeviceHive Proxy WebSocket Kafka Implementation
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

import com.devicehive.exceptions.HiveException;
import com.devicehive.proxy.api.NotificationHandler;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessage;
import com.devicehive.proxy.api.payload.NotificationPayload;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ClientEndpoint(
        decoders = GsonProxyMessageDecoder.class,
        encoders = GsonProxyMessageEncoder.class
)
public class WebSocketKafkaProxyClient extends ProxyClient {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketKafkaProxyClient.class);

    private WebSocketKafkaProxyConfig webSocketKafkaProxyConfig;

    @Autowired
    public void setWebSocketKafkaProxyConfig(WebSocketKafkaProxyConfig webSocketKafkaProxyConfig) {
        this.webSocketKafkaProxyConfig = webSocketKafkaProxyConfig;
    }

    private Map<String, CompletableFuture<ProxyMessage>> futureMap;
    private Map<String, Boolean> ackReceived;
    private Session session;

    public WebSocketKafkaProxyClient(NotificationHandler notificationHandler) {
        super(notificationHandler);
    }

    @Override
    public void start() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI("ws://" + webSocketKafkaProxyConfig.getProxyConnect()));
        } catch (Exception e) {
            logger.error("Error during establishing connection: ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        try {
            session.close();
        } catch (IOException e) {
            logger.error("Error during closing connection: ", e);
        }
    }

    @Override
    public CompletableFuture<ProxyMessage> push(ProxyMessage message) {
        this.session.getAsyncRemote().sendObject(message);

        CompletableFuture<ProxyMessage> future = new CompletableFuture<>();
        futureMap.put(message.getId(), future);
        logger.debug("Message {} was sent", message);
        return future;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.futureMap = new ConcurrentHashMap<>();
        this.ackReceived = new ConcurrentHashMap<>();
        logger.info("New WebSocket session established: {}", session.getId());
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        logger.info("WebSocket session {} closed, close code {}", session.getId(), reason.getCloseCode());
        this.session = null;
        futureMap.clear();
        ackReceived.clear();
    }

    @OnMessage
    public void onMessage(List<ProxyMessage> messages) {
        messages.forEach(message -> {
            String id = message.getId();
            CompletableFuture<ProxyMessage> future = futureMap.get(id);
            if (future != null) {
                if ("ack".equals(message.getType())) {
                    if (message.getStatus() != 0) {
                        throw new HiveException("Acknowledgement failed for request id " + id);
                    }
                    ackReceived.put(id, true);
                    logger.debug("Acknowledgement message {} received for request id {}", message, id);
                } else {
                    if (!ackReceived.getOrDefault(id, false)) {
                        throw new HiveException("No acknowledgement received for request id " + id); // toDo: implement flexible logic for acknowledgement behavior
                    }
                    future.complete(message);
                    futureMap.remove(id);
                    ackReceived.remove(id);
                }
            }

            if ("notif".equals(message.getType()) && message.getAction() == null) {
                NotificationPayload payload = (NotificationPayload) message.getPayload();
                notificationHandler.handle(payload.getValue());
            }
            logger.debug("Message {} was received", message);
        });
    }
}
