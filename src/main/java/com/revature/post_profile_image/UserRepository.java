package com.revature.post_profile_image;

import com.revature.post_profile_image.exceptions.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.List;

/**
 * The UserRepository is a class dedicated to connecting to DynamoDB for the purposes of
 * performing a Read to retrieve a user, and performing an Update with the purpose being to
 * give them a shiny new 'profile_picture'.
 *
 * @author John Callahan
 */
public class UserRepository {

    private final DynamoDbTable<User> userTable;

    public UserRepository(DynamoDbTable<User> userTable) {
        this.userTable = userTable;
    }

    public UserRepository() {
        DynamoDbClient db = DynamoDbClient.builder().httpClient(ApacheHttpClient.create()).build();
        DynamoDbEnhancedClient dbClient = DynamoDbEnhancedClient.builder().dynamoDbClient(db).build();
        userTable = dbClient.table("Users", TableSchema.fromBean(User.class));
    }

    /**
     * Find the user by their ID number in Cognito. Has nothing to do with Cognito.
     * @param id - the partition key that is used to find the user. Without this, it would be extremely difficult to
     *           locate the specific identity within the DynamoDB table.
     * @return - A user object bearing the data sourced from the DynamoDB table. This is so that we can send it back to the
     * handler for proper updating of their profile picture with the URL sourced from S3.
     */
    public User findUserById(String id) {
        AttributeValue val = AttributeValue.builder().s(id).build();
        // Make an expression where '#a' and ':b' are variables. Assign those variables to a string and value, a hashset.
        Expression filter = Expression.builder().expression("#a = :b").putExpressionName("#a", "id").putExpressionValue(":b", val).build();
        ScanEnhancedRequest request = ScanEnhancedRequest.builder().filterExpression(filter).build();

        User user = userTable.scan(request).stream()
                .findFirst()
                .orElseThrow(ResourceNotFoundException::new)
                .items().get(0);

        System.out.println("User found: " + user);
        return user;
    }

    /**
     * Updates the user within the Users table in DynamoDB with a new profile_picture field. This is intended to be
     * a url that can be called to display that user's avatar within the UI.
     * @param user - The user whose profile picture url will be updated and saved.
     * @return - the user object itself for verification within the handler. It is not going to be sent back
     * to the user.
     */
    public User saveUser(User user) { return userTable.updateItem(user); }
}

/**
 * The User POJO is necessary for storing the data received from DynamoDB.
 * It is very much a Data Transfer Object.
 */
@Data
@Builder
@DynamoDbBean
class User {
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

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}

/**
 * SetDocument is necessary because the User returned from DynamoDB has a
 * list of SetDocuments attached to them describing the user's favorite cards
 * and their created list of cards. This is what we are altering.
 */
@Data
@AllArgsConstructor
class SetDocument {
    private String id;
    private String setName;
    private List<Tags> tags;
    private String author;
    private boolean isPublic;
    private int views;
    private int plays;
    private int studies;
    private int favorites;
}

/**
 * Tags is a necessary pojo for persisting data from the DynamoDB to access
 * the users from the database. Without the Tags, it is impossible to handle
 * user data safely.
 */
@Data
@AllArgsConstructor
class Tags {
    private String name;
    private String color;
}
