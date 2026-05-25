package com.mycompany.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import redis.clients.jedis.RedisClient;

public class OneMMCheckboxServer extends WebSocketServer {

    private final RedisClient redisClient;
    static int port = 6969;
    static int CHECKBOXES_COUNT = 100_000_000;
    static String CHECKBOXES_KEY = "checkboxes";

    public OneMMCheckboxServer(int port, RedisClient redisClient) throws UnknownHostException {
        super(new InetSocketAddress(port));
        this.redisClient = redisClient;
    }

    @Override
    public void onClose(WebSocket conn, int arg1, String arg2, boolean arg3) {
        System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " disconnected");
    }

    @Override
    public void onError(WebSocket conn, Exception error) {
        System.out.println(error.getMessage());
        error.printStackTrace();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("message received: " + message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer blob) {
        byte[] message = new byte[blob.remaining()];
        blob.get(message);

        // Minimum valid message:
        // IXM + checkbox id + S + 1 status
        if (message.length < 6) {
            System.out.println("Message too short");
            return;
        }

        // Validate protocol
        if (message[0] != 'I' ||
                message[1] != 'X' ||
                message[2] != 'M') {

            System.out.println("Protocol mismatch");
            return;
        }

        int delimiterIndex = -1;

        // Find S delimiter
        for (int i = 3; i < message.length; i++) {
            if (message[i] == 'S') {
                delimiterIndex = i;
                break;
            }
        }

        if (delimiterIndex == -1) {
            System.out.println("Missing status delimiter");
            return;
        }

        // Ensure status byte exists
        if (delimiterIndex + 1 >= message.length) {
            System.out.println("Missing status byte");
            return;
        }

        // Extract checkbox id
        String checkboxIdStr = new String(
                message,
                3,
                delimiterIndex - 3,
                java.nio.charset.StandardCharsets.UTF_8);

        int checkboxId;

        try {
            checkboxId = Integer.parseInt(checkboxIdStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid checkbox id");
            return;
        }

        byte statusByte = message[delimiterIndex + 1];

        if (statusByte != 0 && statusByte != 1) {
            System.out.println("Invalid status byte");
            return;
        }

        boolean checked = statusByte == 1;

        System.out.println("Message parsed");
        System.out.println("Checkbox number: " + checkboxId);
        System.out.println("Checkbox status: " + checked);

        this.redisClient.setbit(CHECKBOXES_KEY.getBytes(), checkboxId, checked);
        // subscribe to key update events

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        broadcast(bos.toByteArray());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake clientHandshake) {
        conn.send("Successfully connected!");
        byte[] currentBitmapState = this.redisClient.get(CHECKBOXES_KEY.getBytes());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(currentBitmapState);
        } catch (Exception e) {
            e.printStackTrace();
        }

        conn.send(bos.toByteArray());
        System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected successfully");
    }

    @Override
    public void onStart() {
        System.out.println("Server started!");
        // setTcpNoDelay(true);
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        RedisClient jedis = RedisClient.builder().hostAndPort("localhost", 6379).build();

        byte[] initialBytes = new byte[CHECKBOXES_COUNT / 8];
        Arrays.fill(initialBytes, (byte) 0xFF);
        jedis.set(CHECKBOXES_KEY.getBytes(), initialBytes);
        jedis.setbit(CHECKBOXES_KEY.getBytes(), 0, false);
        jedis.setbit(CHECKBOXES_KEY.getBytes(), 1, false);
        jedis.setbit(CHECKBOXES_KEY.getBytes(), 4, false);
        jedis.setbit(CHECKBOXES_KEY.getBytes(), 500, false);
        jedis.setbit(CHECKBOXES_KEY.getBytes(), 2000, false);
        jedis.setbit(CHECKBOXES_KEY.getBytes(), 50000, false);

        // TODO: Refatorar tudo isso aqui

        OneMMCheckboxServer server = new OneMMCheckboxServer(port, jedis);
        server.setReuseAddr(true);
        server.start();
        System.out.printf("Server started on port %d\n", port);

    }
}
