package com.revature.post_profile_image;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.commons.io.IOUtils;
import org.mockito.internal.util.io.IOUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ProfilePictureUploadHandlerTest {

    ProfilePictureUploadHandler sut;



    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        sut = new ProfilePictureUploadHandler();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        sut = null;
    }

    @org.junit.jupiter.api.Test
    void handleRequest_IfGiven_ValidBase64Image() throws IOException {
        // Arrange
        FileInputStream bigChungus = new FileInputStream("src/test/resources/big-base-64.txt");
        String base64 = IOUtils.toString(bigChungus);

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.withBody(base64);
        requestEvent.withHeaders(Collections.singletonMap("Content-Type", "base64"));


        // Act

        // Assert

    }
}