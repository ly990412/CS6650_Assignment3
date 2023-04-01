package org.example;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import java.util.concurrent.CountDownLatch;
public class Client {
    public int successfulRequests = 0;
    public int unsuccessfulRequests = 0;
    public static final int maxSwiperId = 5000;
    public static final int maxSwipeeId = 50000;
    public static final int length = 256;
    public static final int numThreads = 200;
    public static final Integer totalRequests = 500000;
    public CountDownLatch remainingThreads;

    public synchronized void incSuccessfulRequests() { successfulRequests++; }
    public synchronized void incUnsuccessfulRequests() { unsuccessfulRequests++; }

    private void sendAllRequests() throws InterruptedException {
        successfulRequests = 0;
        unsuccessfulRequests = 0;
        int reqPerThread = totalRequests / numThreads;
        remainingThreads = new CountDownLatch(numThreads);

        // Start time
        long start = System.currentTimeMillis();
        // Spawn all threads
        for (int i = 0; i < numThreads; i++) {
            new Thread(new Requester(reqPerThread)).start();
        }
        // Wait for all threads to terminate
        remainingThreads.await();

        long end = System.currentTimeMillis();
        // Wall time (total time taken in seconds)
        double wall = (end - start) * 0.001;
        // Throughput
        double throughput = totalRequests.doubleValue() / wall;

        System.out.println("Successful requests: " + successfulRequests);
        System.out.println("Unsuccessful requests: " + unsuccessfulRequests);
        System.out.println("Total Requests:" + totalRequests);
        System.out.println("Total time:" + wall + " seconds");
        System.out.println("Throughput: " + throughput + " req/sec");
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            // Run multithreaded test
            client.sendAllRequests();
        } catch (Exception e) {
            System.err.println("error: " + e);
            e.printStackTrace();
        }
    }

    // ------------------------------ Requester ------------------------------
    public class Requester implements Runnable {

        public int numRequestsToSend;
        public String swipe;
        public int swiper;
        public int swipee;
        public String comment;

        public Requester(int count) { numRequestsToSend = count; }

        private void setRequestValues() {
            swipe = RandomUtils.nextInt(1, 3) == 1 ? "left" : "right";
            swiper = RandomUtils.nextInt(1, maxSwiperId);
            swipee = RandomUtils.nextInt(1, maxSwipeeId);
            comment = RandomStringUtils.random(length, true, false);
        }

        public HttpPost buildPostRequest() {
            setRequestValues();
            HttpPost ret = new HttpPost( "http://localhost:8080/server_war_exploded/swipe/"+ swipe);
            String jsonPayload = "{ swiper: " + swiper + ",swipee: " + swipee + ",comment: " + comment + " }";
            ret.setEntity(new StringEntity(jsonPayload));
            return ret;
        }

        @Override
        public void run() {
            CloseableHttpClient client = HttpClients.createDefault();
            boolean reqSuccess;
            for (int i = 0; i < numRequestsToSend; i++) {
                HttpPost httpPost = buildPostRequest();
                for (int attempts = 0; attempts < 5; attempts++) {
                    try {
                        reqSuccess = client.execute(httpPost, response -> {
                            return response.getCode() == HttpStatus.SC_CREATED;
                        });
                        if (reqSuccess) {
                            incSuccessfulRequests();
                            break;
                        }
                    } catch (Exception e) {
                        incUnsuccessfulRequests();
                        System.err.println("POST request error");
                        System.out.println(e);
                    }
                }
            }
            try {
                client.close();
            } catch (Exception e) {
                System.err.println("Client close error");
                System.out.println(e);
            }
            remainingThreads.countDown();
        }
    }
}
