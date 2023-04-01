package org.example;
import com.google.gson.Gson;
public abstract class Conumser {
    protected String exchangeName;
    protected String queueName;
    protected RmqConnection rmqConnection;
    protected DatabaseConnector databaseConnector;
    protected Thread[] threadPool;
    protected Gson gson;



    public Conumser (String exchangeName, String queueName, String rmqConnectionName) {
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.rmqConnection = rmqConnection.create(200,"54.185.42.218",
                rmqConnectionName);
        rmqConnection.declare(queueName, true);
        rmqConnection.bind(queueName, exchangeName, "");
        // Establish connection with DynamoDB server
        databaseConnector = DatabaseConnector.createDatabaseConnector();
        // Initialize gson
        this.gson = new Gson();
    }



    // ------------------------------ SwipeMessageJson ------------------------------
    public static class SwipeMessageJson {
        public int swiper;
        public int swipee;
        public boolean like;
    }
}
