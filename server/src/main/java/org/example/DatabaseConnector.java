package org.example;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import java.util.HashMap;
import java.util.List;


public class DatabaseConnector {
    private DynamoDbClient client;
    private DynamoDbEnhancedClient enhancedClient;
    private HashMap<String, TableSchema> tableSchemas;
    public static final String usersTable = "Users";
    public static final String swipeTable = "SwipeGood";
    public DatabaseConnector() {
        Region region = Region.US_WEST_1;
        client = DynamoDbClient.builder()
                .region(region)
                .build();
        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        tableSchemas = new HashMap<>();
        tableSchemas.put(usersTable, TableSchema.fromBean(User.class));
        tableSchemas.put(swipeTable, TableSchema.fromBean(SwipeGood.class));
    }

    public User getGoodUsers(int userId) {
        User res = null;

        try {
            DynamoDbTable<User> table =
                    enhancedClient.table(usersTable, tableSchemas.get(usersTable));
            Key key = Key.builder()
                    .partitionValue(userId)
                    .build();
            res = table.getItem(key);
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }

        return res;
    }

    public SwipeGood getSwipeGood(int Id) {
        SwipeGood res = null;

        try {
            DynamoDbTable<SwipeGood> table =
                    enhancedClient.table(swipeTable, tableSchemas.get(swipeTable));
            Key key = Key.builder()
                    .partitionValue(Id)
                    .build();
            res = table.getItem(key);
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }

        return res;
    }
    public void recordGood(int Id) {
        try {
            DynamoDbTable<SwipeGood> table =
                    enhancedClient.table(swipeTable, tableSchemas.get(swipeTable));
            Key key = Key.builder()
                    .partitionValue(Id)
                    .build();
            SwipeGood userData = table.getItem(key);
            if (userData == null) {
                userData = new SwipeGood();
                userData.setId(Id);
            }
            userData.setGood(userData.getGood() + 1);
            table.updateItem(userData);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void recordBad(int Id) {
        try {
            DynamoDbTable<SwipeGood> table =
                    enhancedClient.table(swipeTable, tableSchemas.get(swipeTable));
            Key key = Key.builder()
                    .partitionValue(Id)
                    .build();
            SwipeGood userData = table.getItem(key);
            if (userData == null) {
                userData = new SwipeGood();
                userData.setId(Id);
            }
            userData.setBad(userData.getBad() + 1);
            table.updateItem(userData);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void updateGoodUsers(int userId, List<Integer> updatedUsers) {
        try {
            DynamoDbTable<User> table =
                    enhancedClient.table(usersTable, tableSchemas.get(usersTable));
            Key key = Key.builder()
                    .partitionValue(userId)
                    .build();
            User userData = table.getItem(key);
            if (userData == null) {
                userData = new User();
                userData.setId(userId);
            }
            userData.setUsers(updatedUsers);
            table.updateItem(userData);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public static DatabaseConnector createDatabaseConnector() {
        return new DatabaseConnector();
    }
}
