package com.revature.post_profile_image;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.revature.post_profile_image.models.User;
import org.apache.commons.fileupload.MultipartStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.*;

/**
 * ProfilePictureUploadHandler is the handler for an AWS Lambda that takes in a base64 encoded string derived from an image
 * and decrypts it, compiling it into an image and persisting that image into an Amazon S3 bucket for safe and easy querying.
 *
 * @author John Callahan
 * @author Mitchell Panenko
 */

public class ProfilePictureUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson mapper = new GsonBuilder().setPrettyPrinting().create();
    private AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion("us-east-2").build();
    private final UserRepository userRepository;

    public ProfilePictureUploadHandler() {
        userRepository = new UserRepository();
    }

    public ProfilePictureUploadHandler(AmazonS3 s3Client, UserRepository userRepository) {
        this.s3Client = s3Client;
        this.userRepository = userRepository;
    }


    /**
     *
     * Handler for the APIGateway Proxy Request Event. Performs decryption of the base64-encoded
     * image and persists it into a public S3 bucket.
     *
     * @param requestEvent: The input from APIGateway sent from the user. Bears an image,
     *                    Which was turned into base64 when it was sent as part of an HTTP Request.
     * @param context: APIGateway's logger and metadata for tracking incoming and outgoing
     *               requests and responses.
     * @return An HTTP reply bearing a message of either success or failure.
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        // bucketName defines which bucket the lambda outputs to. inStream separates the content from the multipart stream.
        final String bucketName = "user-profile-images-bucket";

        LambdaLogger logger = context.getLogger();
        logger.log("Request received at " + LocalDateTime.now());

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization");
        headers.put("Access-Control-Allow-Origin", "*");
        responseEvent.setHeaders(headers);
        try {
            Map<String, String> queryParams = requestEvent.getQueryStringParameters();
            String user_id;

            // header validation. There must be a user_id parameter
            if (queryParams != null) {
                user_id = queryParams.get("user_id");
                logger.log("User verified! user_id: " + user_id);
            } else {
                user_id = "";
            }

            // if no user parameter, return 400.
            if (user_id.trim().equals("") || user_id.isEmpty()) {
                logger.log("Invalid request; there must be a user_id!");
                responseEvent.setStatusCode(400);
                requestEvent.setBody(mapper.toJson("Invalid request; there must be a user_id!"));
                return responseEvent;
            }

            byte[] fileByteArray = requestEvent.getBody().getBytes();

            logger.log("Incoming request body size: " + fileByteArray.length + "\n");

            // validate that the request body is base64, return 400 if not
            boolean isBase64Encoded = requestEvent.getIsBase64Encoded();
            logger.log("Request is base64 encoded: " + isBase64Encoded + "\n");

            if (!isBase64Encoded) {
                logger.log("Invalid request, this is supposed to be a base64 encoded string!");
                responseEvent.setStatusCode(400);
                requestEvent.setBody(mapper.toJson("Invalid request, this is supposed to be a base64 encoded string!"));
                return responseEvent;
            }

            // Decoding the array from base64 to native script.
            logger.log("Decoding file byte array...\n");
            byte[] decodedFileByteBinary = Base64.getDecoder().decode(fileByteArray);

            // take the headers from the APIGatewayProxyRequestEvent
            logger.log("Retrieving content-type header value and extracting the boundary\n");
            Map<String, String> reqHeaders = requestEvent.getHeaders();

            // without a proper content-type header, it cannot be read.
            if (reqHeaders == null || !reqHeaders.containsKey("Content-Type")) {
                logger.log("Could not process request; Missing Content-Type header.");
                responseEvent.setStatusCode(400);
                requestEvent.setBody(mapper.toJson("Could not process request; Missing Content-Type header."));
                return responseEvent;
            }

            // take charsets from the decoded byte array and turn them into image data.
            String contentType = reqHeaders.get("Content-Type");
            byte[] boundary = contentType.split("=")[1].getBytes(StandardCharsets.UTF_8);

            logger.log("Content-type and boundary extracted from request.");
            logger.log("Decoded file byte array: " + new String(decodedFileByteBinary, StandardCharsets.UTF_8) + "\n");

            // create another bytestream from the imagedata to get a finished image.
            logger.log("Writing file data to byte stream\n");
            ByteArrayInputStream content = new ByteArrayInputStream(decodedFileByteBinary);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // create three different streams for the content, boundary, and the decoded data. Skip past the unnecessary preamble.
            MultipartStream multipartStream = new MultipartStream(content, boundary, decodedFileByteBinary.length, null);

            boolean hasNext = multipartStream.skipPreamble();

            while (hasNext) {
                String header = multipartStream.readHeaders();
                logger.log("Headers: " + header);
                multipartStream.readBodyData(output);
                hasNext = multipartStream.readBoundary();
            }

            logger.log("File data written to byte stream!\n");

            logger.log("Preparing file for persistence to s3 bucket " + bucketName + "...\n");
            InputStream inStream = new ByteArrayInputStream(output.toByteArray());

            // objMetadata defines certain aspects of the output image, like the size and type.
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.setContentLength(output.toByteArray().length);
            objMetadata.setContentType(contentType);

            // tag the image with the user_id as a unique name.
            PutObjectResult result = s3Client.putObject(bucketName, user_id, inStream, objMetadata);

            logger.log("File successfully persisted to an S3 Bucket! Hooray!\n");
            logger.log("Result: " + result + "\n");

            logger.log("Fetching the url from the bucket...\n");
            URL pictureUrl = s3Client.getUrl(bucketName, user_id);
            logger.log("URL found! " + pictureUrl.toString() + "\n");

            logger.log("Preparing response object\n");

            // 201: resource successfully created. send back the picture URL.
            responseEvent.setStatusCode(201);
            responseEvent.setBody(pictureUrl.toString());

            // find the user so we may alter them
            logger.log("Fetching user...");
            User user = userRepository.findUserById(user_id, logger);
            logger.log("User found: " + user + "\n");

            // perform the alteration. pictureUrl can never be null, and if one is not found,
            // it results in a NullPointerException. this is perfectly safe.
            logger.log("Performing update of profile_picture...");
            user.setProfilePicture(pictureUrl.toString());
            userRepository.saveUser(user);
            logger.log("Update complete!\n");

        } catch (IOException ioe) {
            logger.log("Error reading byte array!" + ioe.getMessage());
            responseEvent.setStatusCode(500);
            responseEvent.setBody(mapper.toJson("Your image could not be persisted!"));
        } catch (NullPointerException npe) {
            logger.log("Error: Could not find " + npe.getCause() + "!");
            responseEvent.setStatusCode(404);
            responseEvent.setBody(mapper.toJson(npe));
        } catch (Exception e) {
            responseEvent.setStatusCode(500);
            logger.log("An unexpected exception occurred: " + e.getMessage());
        }

        logger.log("Request processing complete. Sending response:: " + responseEvent);
        return responseEvent;
    }
}
