package com.mycompany.app;

import redis.clients.jedis.RedisClient;

public class CheckboxService {
    private final RedisClient redisClient;
    private final String checkboxesKey;
    private final String publishingChannel;
    static int PAGE_ITEM_QTD = 200;

    public CheckboxService(RedisClient redisClient, String checkboxesKey, String publishingChannel) {
        this.redisClient = redisClient;
        this.checkboxesKey = checkboxesKey;
        this.publishingChannel = publishingChannel;
    }

    public void mutateCheckboxValue(ProtocolMutatedMessage message) {
        this.redisClient.setbit(this.checkboxesKey.getBytes(), message.checkboxId, message.value);
        boolean bit = this.redisClient.getbit(this.checkboxesKey.getBytes(), message.checkboxId);
        System.out.println("Checkbox ID: " + message.checkboxId + "value: " + bit);
    }

    public ProtocolPageMessage getPage(int pageNumber) throws Exception {
        if (pageNumber < 1) {
            throw new Exception("Invalid page number");
        }
        int startIndex = ((pageNumber - 1) * PAGE_ITEM_QTD) / 8;
        int endIndex = (pageNumber * PAGE_ITEM_QTD / 8) - 1;
        System.out.println("Start: " + startIndex + ", End: " + endIndex);
        byte[] pageContent = this.redisClient.getrange(
                checkboxesKey.getBytes(),
                startIndex, endIndex);

        return new ProtocolPageMessage(pageNumber, pageContent);
    }
}
