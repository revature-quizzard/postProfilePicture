package com.revature.post_profile_image.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@DynamoDbBean
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

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<SetDocument> getFavoriteSets() {
        return favoriteSets;
    }

    public void setFavoriteSets(List<SetDocument> favoriteSets) {
        this.favoriteSets = favoriteSets;
    }

    public List<SetDocument> getCreatedSets() {
        return createdSets;
    }

    public void setCreatedSets(List<SetDocument> createdSets) {
        this.createdSets = createdSets;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public List<String> getGameRecords() {
        return gameRecords;
    }

    public void setGameRecords(List<String> gameRecords) {
        this.gameRecords = gameRecords;
    }
}