package com.bgu.dsp.awsUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

public class S3Utils {
    private static AmazonS3Client s3;
    final static Logger logger = Logger.getLogger(S3Utils.class);

    static {
        init();
    }

    public static void init() {
        s3 = new AmazonS3Client(new InstanceProfileCredentialsProvider());

        if ("DEV".equals(System.getenv("DSP_MODE")) || "DEV".equals(System.getenv("DSP_MODE_S3"))){
            //TODO is this neccessary on local mock mode?
            AWSCredentials credentials = Utils.getAwsCredentials();
            s3 = new AmazonS3Client(credentials);
            s3.setRegion(Utils.region);

            String host = "localhost";
            int port = 4567;
            String URL = "http://" + host + ":" + port;
            s3.setEndpoint(URL);
            s3.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
            logger.info("Using development S3 with url " + URL);
        }
    }

    /**
     *
     * @param lowerCaseBucketName
     * @return Bucket
     */
    public static Bucket createBucket (String lowerCaseBucketName) {
        return s3.createBucket(lowerCaseBucketName);
    }

    /**
     *
     * @return List<Bucket>
     */
    public static List<Bucket> getBuckets() {
        return s3.listBuckets();
    }

    /**
     *
     * @param bucketName
     * @param prefix
     * @return List<String>
     */
    public static List<String> getFileNames(String bucketName, String prefix) {
        return s3.listObjects(new ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix(prefix))
                .getObjectSummaries()
                .stream().map(S3ObjectSummary::getKey)
                .collect(Collectors.toList());

    }

    /**
     *
     * @param bucket
     * @param key
     * @param file
     * @return bool
     */
    public static boolean uploadFile(Bucket bucket, String key, File file) {
        return uploadFile(bucket.getName(), key, file);
    }

    /**
     *
     * @param bucket
     * @param key
     * @param file
     * @return bool
     */
    public static boolean uploadFile(String bucket, String key, File file) {
        try {
            InputStream stream = new FileInputStream(file);
            return uploadFile(bucket, key, stream, file.length());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean uploadFile(String bucket, String key, InputStream stream, long contentLength){

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);

        try {
            PutObjectResult result = s3.putObject(new PutObjectRequest(bucket, key, stream, metadata));
            return true;
        }
        catch (AmazonClientException e) {
            throw e;
        }

    }

    /**
     *
     * @param bucket
     * @param key
     * @return InputStream
     */
    public static InputStream getFileInputStream(Bucket bucket,String key) {
        return getFileInputStream(bucket.getName(), key);
    }

    /**
     *
     * @param bucketName
     * @param key
     * @return InputStream
     */
    public static InputStream getFileInputStream(String bucketName,String key) {
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
        //System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
        return object.getObjectContent();
    }

    /**
     *
     * @param bucket
     * @param key
     * @return File
     * @throws IOException
     */
    public static File downloadFile(Bucket bucket, String key) throws IOException {
        return downloadFile(bucket.getName(), key);
    }

    /**
     *
     * @param bucketName
     * @param key
     * @return File
     * @throws IOException
     */
    public static File downloadFile(String bucketName, String key) throws IOException {
        File file = new File("aws-"+bucketName+"-"+key);
        file.setWritable(true);
        InputStreamReader sreader = new InputStreamReader(getFileInputStream(bucketName, key));
        BufferedReader breader = new BufferedReader(sreader);
        try (FileWriter writer = new FileWriter(file)) {
            while (true) {
                String line = breader.readLine();
                writer.write(line + System.getProperty("line.separator"));
                if (line == null) break;
            }
        }
        return file;
    }

    /**
     *
     * @param bucketName
     */
    public static void deleteBucket(String bucketName) {
        s3.deleteBucket(bucketName);
    }

    /**
     *
     * @param bucketName
     * @param key
     */
    public static void deleteFile(String bucketName,String key) {
        s3.deleteObject(bucketName, key);
    }

}
