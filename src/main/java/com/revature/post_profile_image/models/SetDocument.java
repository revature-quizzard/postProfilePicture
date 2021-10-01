package com.revature.post_profile_image.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

/**
 * SetDocument is necessary because the User returned from DynamoDB has a
 * list of SetDocuments attached to them describing the user's favorite cards
 * and their created list of cards. This is what we are altering.
 */
@Data
@AllArgsConstructor
@Builder
@DynamoDbBean
public class SetDocument {
    private String id;
    private String setName;
    private List<Tag> tags;
    private String author;
    private boolean isPublic;
    private int views;
    private int plays;
    private int studies;
    private int favorites;

    public SetDocument() {
        super();
    }
}
