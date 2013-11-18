package com.devicehive.client.example.user;


import com.devicehive.client.model.Transport;

import java.io.PrintStream;
import java.net.URI;

public class UserWebSocketExample extends UserExample {

    public UserWebSocketExample(PrintStream out) {
        super(out);
    }

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        UserWebSocketExample example = new UserWebSocketExample(System.out);
        if (args.length < 2) {
            example.printUsage();
        } else {
            URI rest = URI.create(args[0]);
            URI websocket = URI.create(args[1]);
            example.run(rest, websocket, Transport.REST_ONLY);
        }
    }
}