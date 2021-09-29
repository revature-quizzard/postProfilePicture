package com.revature.post_profile_image;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.revature.post_profile_image.stubs.TestLogger;
import org.apache.commons.io.IOUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfilePictureUploadHandlerTest {

    static TestLogger testLogger;

    ProfilePictureUploadHandler sut;
    Context mockContext;
    AmazonS3 mockS3Client;

    @org.junit.jupiter.api.BeforeAll
    public static void beforeAll() { testLogger = new TestLogger(); }

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws MalformedURLException {
        mockS3Client = mock(AmazonS3.class);
        sut = new ProfilePictureUploadHandler(mockS3Client);

        mockContext = mock(Context.class);
        when(mockContext.getLogger()).thenReturn(testLogger);

        URLStreamHandler testStreamHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return null;
            }
        };
        URL testUrl = new URL("", "", 27017, "", testStreamHandler);
        PutObjectResult testResult = new PutObjectResult();
        when(mockS3Client.putObject(anyString(), anyString(), any(InputStream.class), any())).thenReturn(testResult);
        when(mockS3Client.getUrl(anyString(), anyString())).thenReturn(testUrl);
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
        mockRequestEvent.withQueryStringParameters(Collections.singletonMap("user_id", "valid"));
        mockRequestEvent.withBody(base64);
        mockRequestEvent.setIsBase64Encoded(true);
        mockRequestEvent.setHeaders(headers);

        // Act
        APIGatewayProxyResponseEvent responseEvent = sut.handleRequest(mockRequestEvent, mockContext);

        // Assert
        assertEquals(201, responseEvent.getStatusCode());
    }

    @org.junit.jupiter.api.Test
    void handleRequest_Return400_IfGiven_ValidBase64Image_withNoContentTypeHeader() throws IOException {
        // Arrange
        FileInputStream bigChungus = new FileInputStream("src/test/resources/big-base-64.txt");
        String base64 = IOUtils.toString(bigChungus);

        APIGatewayProxyRequestEvent mockRequestEvent = new APIGatewayProxyRequestEvent();
        mockRequestEvent.withPath("/users/images");
        mockRequestEvent.withHttpMethod("POST");
        mockRequestEvent.withQueryStringParameters(Collections.singletonMap("user_id", "valid"));
        mockRequestEvent.withBody(base64);
        mockRequestEvent.setIsBase64Encoded(true);
        mockRequestEvent.withQueryStringParameters(Collections.singletonMap("user_id", "valid"));

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
        mockRequestEvent.withQueryStringParameters(Collections.singletonMap("user_id", "valid"));
        mockRequestEvent.withBody(base64);
        mockRequestEvent.setIsBase64Encoded(false);

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