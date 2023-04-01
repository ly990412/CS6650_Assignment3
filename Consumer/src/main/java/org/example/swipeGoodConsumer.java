package org.example;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
public class swipeGoodConsumer extends Conumser{
    private ConcurrentHashMap<Integer, SwipeGood> bySwiperId;

    public swipeGoodConsumer() {
        super("swipe_exchange", "good_users_queue", "Swipe Good Consumer");
        bySwiperId = new ConcurrentHashMap<>(5000);
        // Initialize the map
        for (int i = 0; i < 5000; i++) {
            bySwiperId.put(i, new SwipeGood());
            bySwiperId.get(i).setId(i);
        }
        // Initialize the thread pool
        this.threadPool = new Thread[200];
        for (int i = 0; i < 200; i++) {
            threadPool[i] = new Thread(new Consumer());
        }
        System.out.println("Consumer for queue successfully launched. " );
    }

    public void updateStats(HashMap<Integer, SwipeGood> buffer, Integer swiperId, int swipeeId, boolean isLike) {
        bySwiperId.computeIfPresent(swiperId, (k, v) -> {
            if (isLike) {
                v.setGood(v.getGood() + 1);
            } else {
                v.setBad(v.getBad() + 1);
            }
            databaseConnector.updateSwipeGood("SwipeGood", swiperId, isLike);
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
        swipeGoodConsumer consumer = new swipeGoodConsumer();
        consumer.consume();
    }

    // ------------------------------ Consumer ------------------------------
    public class Consumer implements Runnable {

        HashMap<Integer, SwipeGood> buffer;

        @Override
        public void run() {
            Channel channel = rmqConnection.borrowChannel();
            buffer = new HashMap<>(25);
            try {
                int prefetchCount = 200;
                channel.basicQos(prefetchCount);
                channel.addShutdownListener(e -> System.err.println(e.getMessage()));
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
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
