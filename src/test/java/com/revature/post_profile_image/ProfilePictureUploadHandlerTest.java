package com.revature.post_profile_image;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.revature.post_profile_image.stubs.TestLogger;
import org.apache.commons.io.IOUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfilePictureUploadHandlerTest {

    static TestLogger testLogger;

    ProfilePictureUploadHandler sut;
    Context mockContext;

    @org.junit.jupiter.api.BeforeAll
    public static void beforeAll() { testLogger = new TestLogger(); }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        sut = new ProfilePictureUploadHandler();

        mockContext = mock(Context.class);
        when(mockContext.getLogger()).thenReturn(testLogger);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        sut = null;
        mockContext = null;
    }

    @org.junit.jupiter.api.Test
    void handleRequest_IfGiven_ValidBase64Image_Return500_S3ClientNotFound() throws IOException {
        // Arrange
        FileInputStream bigChungus = new FileInputStream("src/test/resources/big-base-64.txt");
        String base64 = IOUtils.toString(bigChungus);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain; charset=ISO-5589-1");
        headers.put("Content-transfer-encoding", "base64");

        APIGatewayProxyRequestEvent mockRequestEvent = new APIGatewayProxyRequestEvent();
        mockRequestEvent.withPath("/users/images");
        mockRequestEvent.withHttpMethod("POST");
        mockRequestEvent.withPathParameters(Collections.singletonMap("user_id", "valid"));
        mockRequestEvent.withBody(base64);
        mockRequestEvent.setIsBase64Encoded(true);
        mockRequestEvent.setHeaders(headers);
        mockRequestEvent.withQueryStringParameters(null);

        // Act
        APIGatewayProxyResponseEvent responseEvent = sut.handleRequest(mockRequestEvent, mockContext);

        // Assert
        assertEquals(500, responseEvent.getStatusCode());
    }

    @org.junit.jupiter.api.Test
    void handleRequest_Return400_IfGiven_ValidBase64Image_withNoContentTypeHeader() throws IOException {
        // Arrange
        FileInputStream bigChungus = new FileInputStream("src/test/resources/big-base-64.txt");
        String base64 = IOUtils.toString(bigChungus);

        APIGatewayProxyRequestEvent mockRequestEvent = new APIGatewayProxyRequestEvent();
        mockRequestEvent.withPath("/users/images");
        mockRequestEvent.withHttpMethod("POST");
        mockRequestEvent.withPathParameters(Collections.singletonMap("user_id", "valid"));
        mockRequestEvent.withBody(base64);
        mockRequestEvent.setIsBase64Encoded(true);
        mockRequestEvent.withQueryStringParameters(null);

        // Act
        APIGatewayProxyResponseEvent responseEvent = sut.handleRequest(mockRequestEvent, mockContext);

        // Assert
        assertEquals(400, responseEvent.getStatusCode());
    }

    @org.junit.jupiter.api.Test
    void handleRequest_Return400_IfGiven_InvalidContent() throws IOException {
        // Arrange
        FileInputStream bigChungus = new FileInputStream("src/test/resources/big-base-64.txt");
        String base64 = IOUtils.toString(bigChungus);

        APIGatewayProxyRequestEvent mockRequestEvent = new APIGatewayProxyRequestEvent();
        mockRequestEvent.withPath("/users/images");
        mockRequestEvent.withHttpMethod("POST");
        mockRequestEvent.withPathParameters(Collections.singletonMap("user_id", "valid"));
        mockRequestEvent.withBody(base64);
        mockRequestEvent.setIsBase64Encoded(false);
        mockRequestEvent.withQueryStringParameters(null);

        // Act
        APIGatewayProxyResponseEvent responseEvent = sut.handleRequest(mockRequestEvent, mockContext);

        // Assert
        assertEquals(400, responseEvent.getStatusCode());
    }

    @org.junit.jupiter.api.Test
    void handleRequest_Return400_IfGiven_InvalidUserId() throws IOException {
        // Arrange
        FileInputStream bigChungus = new FileInputStream("src/test/resources/big-base-64.txt");
        String base64 = IOUtils.toString(bigChungus);

        APIGatewayProxyRequestEvent mockRequestEvent = new APIGatewayProxyRequestEvent();
        mockRequestEvent.withPath("/users/images");
        mockRequestEvent.withHttpMethod("POST");
        mockRequestEvent.withBody(base64);
        mockRequestEvent.setIsBase64Encoded(false);
        mockRequestEvent.withQueryStringParameters(null);

        // Act
        APIGatewayProxyResponseEvent responseEvent = sut.handleRequest(mockRequestEvent, mockContext);

        // Assert
        assertEquals(400, responseEvent.getStatusCode());
    }
}