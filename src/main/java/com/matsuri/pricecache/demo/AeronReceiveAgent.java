
package com.matsuri.pricecache.demo;

import io.aeron.Subscription;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;

import java.util.concurrent.atomic.AtomicInteger;

public class AeronReceiveAgent implements Agent {
    private final Subscription subscription;
    private final AtomicInteger messageCount = new AtomicInteger(0);

    public AeronReceiveAgent(final Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public int doWork() throws Exception {
        subscription.poll(this::handler, 100);
        return 0;
    }

    private void handler(final DirectBuffer buffer, final int offset, final int length, final Header header) {
        final String jsonValue = buffer.getStringWithoutLengthUtf8(offset, length);
        messageCount.incrementAndGet();
        System.out.println("Received message no. " + messageCount.get() + " : " + jsonValue);
    }

    @Override
    public void onClose() {
        System.out.println("Prices received successfully. Count: " + messageCount.get());
        Agent.super.onClose();
    }

    @Override
    public String roleName() {
        return "receiver";
    }
}