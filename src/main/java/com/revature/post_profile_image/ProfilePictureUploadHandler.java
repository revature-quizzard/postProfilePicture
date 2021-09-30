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
import org.apache.commons.lang3.ObjectUtils;

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
 * ProfilePictureUploadHandler is the handler for an AWS Lambda that takes in a base64 encoded string derived from an image
 * and decrypts it, compiling it into an image and persisting that image into an Amazon S3 bucket for safe and easy querying.
 *
 * @author John Callahan
 * @author Mitchell Panenko
 */
public class ProfilePictureUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson mapper = new GsonBuilder().setPrettyPrinting().create();
    private AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion("us-east-2").build();
    private UserRepository userRepo = new UserRepository();

    public ProfilePictureUploadHandler(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public ProfilePictureUploadHandler() {}

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

        LambdaLogger logger = context.getLogger();
        logger.log("Request received at " + LocalDateTime.now());

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
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

            logger.log("Incoming request body size: " + fileByteArray.length);

            // validate that the request body is base64, return 400 if not
            boolean isBase64Encoded = requestEvent.getIsBase64Encoded();
            logger.log("Request is base64 encoded: " + isBase64Encoded);

            if (!isBase64Encoded) {
                logger.log("Invalid request, this is supposed to be a base64 encoded string!");
                responseEvent.setStatusCode(400);
                requestEvent.setBody(mapper.toJson("Invalid request, this is supposed to be a base64 encoded string!"));
                return responseEvent;
            }

            // Decoding the array from base64 to native script.
            logger.log("Decoding file byte array...");
            byte[] decodedFileByteBinary = Base64.getDecoder().decode(fileByteArray);

            // take the headers from the APIGatewayProxyRequestEvent
            logger.log("Retrieving content-type header value and extracting the boundary");
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
            logger.log("Writing file data to byte stream");
            ByteArrayInputStream content = new ByteArrayInputStream(decodedFileByteBinary);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // parse content stream to discover the mimeType. Necessary for knowing what to take from the s3.
            logger.log("Checking the mimeType...");
            String mimeType = URLConnection.guessContentTypeFromStream(content); //mimeType is something like "image/jpeg"
            String delimiter = "[/]";
            String[] tokens = mimeType.split(delimiter);
            String fileExtension = tokens[1];
            logger.log("mimeType discovered! " + fileExtension);

            // create three different streams for the content, boundary, and the decoded data. Skip past the unnecessary preamble.
            MultipartStream multipartStream = new MultipartStream(content, boundary, decodedFileByteBinary.length, null);

            boolean hasNext = multipartStream.skipPreamble();

            while (hasNext) {
                String header = multipartStream.readHeaders();
                logger.log("Headers: " + header);
                multipartStream.readBodyData(output);
                hasNext = multipartStream.readBoundary();
            }

            logger.log("File data written to byte stream!");

            // bucketName defines which bucket the lambda outputs to. inStream separates the content from the multipart stream.
            String bucketName = "user-profile-images-bucket";
            logger.log("Preparing file for persistence to s3 bucket " + bucketName + "...");
            InputStream inStream = new ByteArrayInputStream(output.toByteArray());

            // objMetadata defines certain aspects of the output image, like the size and type.
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.setContentLength(output.toByteArray().length);
            objMetadata.setContentType(contentType);

            // tag the image with the user_id as a unique name.
            PutObjectResult result = s3Client.putObject(bucketName, user_id, inStream, objMetadata);

            logger.log("File successfully persisted to an S3 Bucket! Hooray!");
            logger.log("Result: " + result);

            logger.log("Fetching the url from the bucket...");
            URL pictureUrl = s3Client.getUrl(bucketName, user_id + "." + fileExtension);
            logger.log("URL found! " + pictureUrl.toString());

            logger.log("Preparing response object");

            // 201: resource successfully created. send back the picture URL.
            responseEvent.setStatusCode(201);
            responseEvent.setBody(pictureUrl.toString());

            // find the user so we may alter them
            logger.log("Fetching user...");
            User user = userRepo.findUserById(user_id);
            logger.log("User found: " + user);

            // perform the alteration. pictureUrl can never be null, and if one is not found,
            // it results in a NullPointerException. this is perfectly safe.
            logger.log("Performing update of profile_picture...");
            user.setProfile_picture(pictureUrl.toString());
            userRepo.saveUser(user);
            logger.log("Update complete!");

        } catch (IOException ioe) {
            logger.log("Error reading byte array!" + ioe.getMessage());
            responseEvent.setStatusCode(500);
            responseEvent.setBody(mapper.toJson("Your image could not be persisted!"));
        } catch (NullPointerException npe) {
            logger.log("Error: Could not find " + npe.getCause() + "!");
            responseEvent.setStatusCode(504);
            responseEvent.setBody(mapper.toJson(npe));
        } catch (Exception e) {
            responseEvent.setStatusCode(500);
            logger.log("An unexpected exception occurred! " + e.getMessage());
        }

        logger.log("Request processing complete. Sending response:: " + responseEvent);
        return responseEvent;
    }
}
