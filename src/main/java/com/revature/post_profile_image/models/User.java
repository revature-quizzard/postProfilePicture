package com.revature.post_profile_image.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import java.util.List;

/**
 * The User POJO is necessary for storing the data received from DynamoDB.
 * It is very much a Data Transfer Object.
 */
@Data
@Builder
@DynamoDbBean
@AllArgsConstructor
public class User {
    private String id;
    private String username;
    private List<SetDocument> favoriteSets;
    private List<SetDocument> createdSets;
    private String profilePicture;
    private int points;
    private int wins;
    private int losses;
    private String registrationDate;
    private List<String> gameRecords;

    public User() {
        super();
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}