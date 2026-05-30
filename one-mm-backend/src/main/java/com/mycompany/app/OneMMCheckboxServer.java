package com.mycompany.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.RedisClient;

public class OneMMCheckboxServer extends WebSocketServer {

    private final RedisClient redisClient;
    private final RedisClient redisConsumer;
    static int PORT = 6969;
    static int REDIS_PORT = 6379;
    static int CHECKBOXES_COUNT = 100_000_000;
    static String CHECKBOXES_KEY = "checkboxes";
    static String CHANNEL = "bitmap-updates";
    private ProtocolService parseMessageService;
    private CheckboxService checkboxService;

    public OneMMCheckboxServer(int port, RedisClient redisClient, RedisClient redisConsumer)
            throws UnknownHostException {
        super(new InetSocketAddress(port));

        this.redisClient = redisClient;
        this.redisConsumer = redisClient;
        this.parseMessageService = new ProtocolService();
        this.checkboxService = new CheckboxService(redisClient, CHECKBOXES_KEY, CHANNEL);

        new Thread(() -> {
            this.redisConsumer.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    System.out.println("Received message: " + message + " in channel: " + channel);

                    broadcast(gzipMessage(message.getBytes()));
                }
            }, CHANNEL);
        }).start();
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
        try {
            System.out.println("Received message: " + new String(blob.array(), StandardCharsets.US_ASCII));
            // ProtocolDecodedMessage parsedMessage = this.parseMessageService.Parse(blob);
            // this.checkboxService.update(parsedMessage, true);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake clientHandshake) {
        byte[] currentBitmapState = this.redisClient.getrange(
                CHECKBOXES_KEY.getBytes(),
                0,
                2000);

        ProtocolPageMessage message = new ProtocolPageMessage(1, currentBitmapState);
        // conn.send(this.gzipMessage(message.getRaw()));
        conn.send(message.getRaw());

        System.out.println(
                conn.getRemoteSocketAddress().getAddress().getHostAddress()
                        + " connected successfully");
    }

    @Override
    public void onStart() {
        System.out.println("Server started!");
        setTcpNoDelay(true);
    }

    private byte[] gzipMessage(byte[] message) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        RedisClient jedis = RedisClient.builder().hostAndPort("localhost", REDIS_PORT).build();
        RedisClient jedisConsumer = RedisClient.builder().hostAndPort("localhost", REDIS_PORT).build();

        byte[] initialBytes = new byte[CHECKBOXES_COUNT / 8];
        Arrays.fill(initialBytes, (byte) 0xFF);
        jedis.set(CHECKBOXES_KEY.getBytes(), initialBytes);

        OneMMCheckboxServer server = new OneMMCheckboxServer(PORT, jedis, jedisConsumer);
        server.setReuseAddr(true);
        server.start();
        System.out.printf("Server started on port %d\n", PORT);

    }
}
