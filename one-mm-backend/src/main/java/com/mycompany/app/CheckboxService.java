package com.mycompany.app;

import redis.clients.jedis.RedisClient;

public class CheckboxService {
    private final RedisClient redisClient;
    private final String updateKey;
    private final String publishingChannel;

    public CheckboxService(RedisClient redisClient, String updateKey, String publishingChannel) {
        this.redisClient = redisClient;
        this.updateKey = updateKey;
        this.publishingChannel = publishingChannel;
    }

    public void update(ParsedMessage message, Boolean publish) {
        this.redisClient.setbit(this.updateKey.getBytes(), message.CheckboxId, message.Value);

        if (publish) {
            this.redisClient.publish(this.publishingChannel, message.Raw);
        }
    }

    public void update(ParsedMessage message) {
        this.redisClient.setbit(this.updateKey.getBytes(), message.CheckboxId, message.Value);
    }
}
