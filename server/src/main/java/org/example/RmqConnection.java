package org.example;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
public class RmqConnection {
    private int numChannels;
    private Connection connection;
    private BlockingQueue<Channel> pool;
    public static final String rmqHost = "a3_host";
    public static final String rmqUsername = "adminYuLiu";
    public static final String rmqPassword = "ly990412";

    public RmqConnection(int numChannels, Connection connection) {
        this.numChannels = numChannels;
        this.connection = connection;
        pool = new LinkedBlockingQueue<>(numChannels);
        for (int i = 0; i < numChannels; i++) {
            Channel newChannel;
            try {
                newChannel = connection.createChannel();
                pool.put(newChannel);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Channel borrowChannel() {
        try {
            return pool.take();
        } catch (Exception e) {
            System.out.println("Unable to borrow channel due to exception: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void returnChannel(Channel channel) {
        if (channel != null) {
            pool.add(channel);
        }
    }

    public void exchange(String exchangeName, String exchangeType, boolean durable) {
        Channel channel = borrowChannel();
        try {
            channel.exchangeDeclare(exchangeName, exchangeType, durable);
        } catch (IOException e) {

            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            returnChannel(channel);
        }
    }

    public void declare(String queueName, boolean durable) {
        Channel channel = borrowChannel();
        try {
            channel.queueDeclare(queueName, durable, false, false, null);
        } catch (IOException e) {

            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            returnChannel(channel);
        }
    }

    public void bind(String queueName, String exchangeName, String routingKey) {
        Channel channel = borrowChannel();
        try {

            channel.queueBind(queueName, exchangeName, routingKey);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            returnChannel(channel);
        }
    }

    public static RmqConnection create(int numChannels, String rmqHostName,
                                                               String connectionName) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setHost(rmqHostName);
            connectionFactory.setUsername(rmqUsername);
            connectionFactory.setPassword(rmqPassword);
            connectionFactory.setVirtualHost(rmqHost);
            Connection connection = connectionFactory.newConnection(connectionName);
            return new RmqConnection(numChannels, connection);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
