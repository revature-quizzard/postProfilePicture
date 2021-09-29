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
import java.util.HashMap;
import java.util.Map;

/**
 * ProfilePictureUploadHandler is a Java-based AWS Lambda program whose sole job
 * is to take an image and shepherd it into a database for a grateful user. It is an
 * abstraction for a massive amount of heavy lifting in terms of conversion between
 * image and text. And it is beautiful.
 *
 * @author John Callahan
 */
public class ProfilePictureUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson mapper = new GsonBuilder().setPrettyPrinting().create();
    private AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion("us-east-2").build();

    public ProfilePictureUploadHandler(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public ProfilePictureUploadHandler() {}

    /**
     *
     * Handler for the APIGateway Proxy Request Event. Common among all lambdas.
     * Encrypts an image in base 64 for easy sending to a s3 bucket.
     *
     * @param requestEvent: The requesting event obtained from APIGateway,
     * likely bearing an image ripe for turning into a byte array.
     * @param context: The context surrounding
     * @return responseEvent: An HTTP reply bearing a message of either success or failure.
     * If all goes well, it returns a success message that the image was indeed persisted in
     * Amazon S3.
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        LambdaLogger logger = context.getLogger();
        logger.log("Request received at " + LocalDateTime.now());

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        try {
            Map<String, String> params = requestEvent.getPathParameters();
            String user_id;

            // header validation
            if (params != null) {
                user_id = params.get("user_id");
                logger.log("User verified! user_id: " + user_id);
            } else {
                user_id = "";
            }

            // Return 400 over invalid user request with no user_id header.
            if (user_id.trim().equals("") || user_id.isEmpty()) {
                logger.log("Invalid request; there must be a user_id!");
                responseEvent.setStatusCode(400);
                return responseEvent;
            }

            byte[] fileByteArray = requestEvent.getBody().getBytes();

            logger.log("Incoming request body size: " + fileByteArray.length);

            // Base64 Validation
            boolean isBase64Encoded = requestEvent.getIsBase64Encoded();
            logger.log("Request is base64 encoded: " + isBase64Encoded);

            if (!isBase64Encoded) {
                logger.log("Invalid request, this is supposed to be a base64 encoded string!");
                responseEvent.setStatusCode(400);
                return responseEvent;
            }

            // Decoding the string...
            logger.log("Decoding file byte array...");
            byte[] decodedFileByteBinary = Base64.getDecoder().decode(fileByteArray);

            // Own the headers
            logger.log("Retrieving content-type header value and extracting the boundary");
            Map<String, String> reqHeaders = requestEvent.getHeaders();

            if (reqHeaders==null || !reqHeaders.containsKey("Content-Type")) {
                logger.log("Could not process request; Missing Content-Type header.");
                responseEvent.setStatusCode(400);
                return responseEvent;
            }

            // Split the string into charsets
            String contentType = reqHeaders.get("Content-Type");
            byte[] boundary = contentType.split("=")[1].getBytes(StandardCharsets.UTF_8);

            logger.log("Content-type and boundary extracted from request.");
            logger.log("Decoded file byte array: " + new String(decodedFileByteBinary, StandardCharsets.UTF_8) + "\n");

            // Write the file to a byte stream for processing.
            logger.log("Writing file data to byte stream");
            ByteArrayInputStream content = new ByteArrayInputStream(decodedFileByteBinary);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // Parse input stream to discover the mimeType.
            String mimeType = URLConnection.guessContentTypeFromStream(content); //mimeType is something like "image/jpeg"
            String delimiter="[/]";
            String[] tokens = mimeType.split(delimiter);
            String fileExtension = tokens[1];
            logger.log("mimeType discovered! " + fileExtension);

            // Stream over the data and write it to a decrypted byte stream.
            MultipartStream multipartStream = new MultipartStream(content, boundary, decodedFileByteBinary.length, null);

            boolean hasNext = multipartStream.skipPreamble();

            while (hasNext) {
                String header = multipartStream.readHeaders();
                logger.log("Headers: " + header);
                multipartStream.readBodyData(output);
                hasNext = multipartStream.readBoundary();
            }

            logger.log("File data written to byte stream!");

            // Bucketname variable pointing to the user-profile-images-bucket.
            String bucketName = "user-profile-images-bucket";
            logger.log("Preparing file for persistence to s3 bucket " + bucketName + "...");
            InputStream inStream = new ByteArrayInputStream(output.toByteArray());

            // Generate object metadata.
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.setContentLength(output.toByteArray().length);
            objMetadata.setContentType(contentType);

            // Tag the image with the user_id as a name, to prevent any possible query problems.
            PutObjectResult result = s3Client.putObject(bucketName, user_id, inStream, objMetadata);
            URL pictureUrl = s3Client.getUrl(bucketName, user_id + "." + fileExtension);

            logger.log("File successfully persisted to an S3 Bucket! Hooray!");
            logger.log("Result: " + result);

            logger.log("Preparing response object");
            // Send back a 201 with the URL for profilePicture persistence purposes.
            responseEvent.setStatusCode(201);
            responseEvent.setBody(pictureUrl.toString());

            // Generate the body of the response. It will be a JSON of the key value pairs from respbody
            Map<String, String> respBody = new HashMap<>();
            respBody.put("status", "uploaded");
            respBody.put("filename", user_id);
            responseEvent.setBody(mapper.toJson(respBody));

        } catch(IOException ioe) {
            logger.log("Error reading byte array!" + ioe.getMessage());
            responseEvent.setStatusCode(500);
            return responseEvent;
        } catch (Exception e) {
            responseEvent.setStatusCode(500);
            logger.log("An unexpected exception occurred! " + e.getMessage());
        }

        logger.log("Request processing complete. Sending response:: " + responseEvent);
        return responseEvent;
    }
}
