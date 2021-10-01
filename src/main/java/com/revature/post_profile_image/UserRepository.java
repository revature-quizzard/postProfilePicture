package com.revature.post_profile_image;

import com.revature.post_profile_image.exceptions.ResourceNotFoundException;
import com.revature.post_profile_image.models.User;
import lombok.SneakyThrows;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * The UserRepository is a class dedicated to connecting to DynamoDB for the purposes of
 * performing a Read to retrieve a user, and performing an Update with the purpose being to
 * give them a shiny new 'profile_picture'.
 *
 * @author John Callahan
 */
public class UserRepository {

    public final DynamoDbTable<User> userTable;

    public UserRepository() {
        DynamoDbClient db = DynamoDbClient.builder().httpClient(ApacheHttpClient.create()).build();
        DynamoDbEnhancedClient dbClient = DynamoDbEnhancedClient.builder().dynamoDbClient(db).build();
        userTable = dbClient.table("Users", TableSchema.fromBean(User.class));
    }

    @SneakyThrows
    public User findUserById(String id) {
        AttributeValue value = AttributeValue.builder().s(id).build();
        Expression filter = Expression.builder().expression("#a = :b").putExpressionName("#a", "id").putExpressionValue(":b", value).build();
        ScanEnhancedRequest request = ScanEnhancedRequest.builder().filterExpression(filter).build();
        try {
            User user = userTable.scan(request).stream().findFirst().orElseThrow(RuntimeException::new).items().get(0);
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Updates the user within the Users table in DynamoDB with a new profile_picture field. This is intended to be
     * a url that can be called to display that user's avatar within the UI.
     * @param user - The user whose profile picture url will be updated and saved.
     * @return - the user object itself for verification within the handler. It is not going to be sent back
     * to the user.
     */
    @SneakyThrows
    public User saveUser(User user) { return userTable.updateItem(user); }
}
