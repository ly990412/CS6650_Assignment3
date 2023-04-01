package org.example;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
public class userConsumer extends Conumser{
    private ConcurrentHashMap<Integer, User> bySwiperId;

    public userConsumer() {
        super("swipe_exchange", "good_users_queue", "Users Consumer");
        bySwiperId = new ConcurrentHashMap<>(5000);
        for (int i = 0; i < 5000; i++) {
            bySwiperId.put(i, new User());
            bySwiperId.get(i).setId(i);
            bySwiperId.get(i).setUsers(new HashSet<>(100));
        }
        this.threadPool = new Thread[200];
        for (int i = 0; i < 200; i++) {
            threadPool[i] = new Thread(new Consumer());
        }
        System.out.println("Consumer for queue successfully launched. " );
    }
    public void updateStats(HashMap<Integer, User> buffer, Integer swiperId, int swipeeId, boolean liked) {
        bySwiperId.computeIfPresent(swiperId, (k, v) -> {
            if (!liked || v.getUsers().size() == 200 || v.getUsers().contains(swipeeId)) {
                return v;
            }
            databaseConnector.updateGoodUsers("GoodUsers", swiperId, swipeeId);
            return v;
        });
    }

    public void consume() {
        int numThreads = threadPool.length;
        for (int i = 0; i < numThreads; i++) {
            threadPool[i].start();
        }
    }

    public static void main(String[] argv) {
        userConsumer consumer = new userConsumer();
        consumer.consume();
    }

    public class Consumer implements Runnable {

        HashMap<Integer, User> buffer;

        @Override
        public void run() {
            Channel channel = rmqConnection.borrowChannel();
            buffer = new HashMap<>(25);
            try {
                int prefetchCount = 200;
                channel.basicQos(prefetchCount);
                channel.addShutdownListener(e -> System.err.println(e.getMessage()));
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String msg = new String(delivery.getBody());
                    SwipeMessageJson json = gson.fromJson(msg, SwipeMessageJson.class);
                    updateStats(buffer, json.swiper, json.swipee, json.like);
                };
                boolean autoAck = true;
                channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> { });
            } catch (Exception e) {
                System.err.println("Consumer thread error");
                e.printStackTrace();
            } finally {
                rmqConnection.returnChannel(channel);
            }
        }
    }
}
