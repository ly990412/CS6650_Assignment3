package org.example;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.util.ArrayList;
import java.util.Collections;


public class DatabaseConnector {
    private DynamoDbClient client;
    private DynamoDbEnhancedClient enhancedClient;
    public DatabaseConnector() {
        Region region = Region.US_WEST_1;
        client = DynamoDbClient.builder()
                .region(region)
                .build();
        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }
    public void updateSwipeGood(String table, int userId, boolean isGood) {
        String pK = String.valueOf(userId);
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(table)
                .key(Collections.singletonMap("Id", AttributeValue.builder().n(pK).build()))
                .updateExpression("ADD #attribute :value")
                .expressionAttributeNames(Collections.singletonMap(
                        "#attribiute", isGood ? "good" : "bad"))
                .expressionAttributeValues(Collections.singletonMap(
                        ":value", AttributeValue.builder().n("1").build()))
                .build();
        try {
            UpdateItemResponse response = client.updateItem(updateRequest);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public void updateGoodUsers(String table, int userId, int swipeeId) {
        String pK = String.valueOf(userId);
        AttributeValue addedUser = AttributeValue.builder().ns(String.valueOf(swipeeId)).build();
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(table)
                .key(Collections.singletonMap("Id", AttributeValue.builder().n(pK).build()))
                .updateExpression("ADD #attribute :good")
                .expressionAttributeNames(Collections.singletonMap("#attribute", "users"))
                .expressionAttributeValues(Collections.singletonMap(":good", addedUser))
                .build();
        try {
            UpdateItemResponse response = client.updateItem(updateRequest);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    public static DatabaseConnector createDatabaseConnector() {
        return new DatabaseConnector();
    }
}
