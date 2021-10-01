package com.revature.post_profile_image.models;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@Data
@DynamoDbBean
public class SetDocument {
    private String id;
    private String setName;
    private List<Tag> tags;
    private boolean isPublic;
    private String author;
    private int views;
    private int plays;
    private int studies;
    private int favorites;
}