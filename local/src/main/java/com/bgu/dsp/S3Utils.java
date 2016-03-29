package com.bgu.dsp;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by thinkPAD on 3/29/2016.
 */

public class S3Utils {
    private static AmazonS3Client s3;
    private static Region region;

    static {
        init();
    }

    public static void init() {
        AWSCredentials credentials = getAwsCredentials();
        s3 = new AmazonS3Client(credentials);
        region = Region.getRegion(Regions.US_EAST_1);
        s3.setRegion(region);
    }

    public static AWSCredentials getAwsCredentials() {
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        }
        catch (Exception e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. ",e);
        }
        return credentials;
    }

    public static Bucket createBucket (String bucketName) {
        return s3.createBucket(bucketName);
    }

    public static Bucket createBucket (String bucketName, Region region) {
        return s3.createBucket(bucketName,region.getName());
    }

    public static List<Bucket> getBuckets() {
        return s3.listBuckets();
    }

    public static List<String> getObjectNames(String bucketName,String prefix) {
        return s3.listObjects(new ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix(prefix))
                .getObjectSummaries()
                .stream().map(S3ObjectSummary::getKey)
                .collect(Collectors.toList());

    }

    public static boolean uploadFile(Bucket bucket, String key, File file) {
        return uploadFile(bucket.getName(), key, file);
    }

    public static boolean uploadFile(String bucket, String key, File file) {
        PutObjectResult result = s3.putObject(new PutObjectRequest(bucket, key, file));
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(Paths.get(file.getPath()));
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] digest = md.digest();
                String s = new String(digest);
                return s.equals(result.getContentMd5());
            }
            catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            return false;
        }
    }

    public static InputStream getFileInputStream(Bucket bucket,String key) {
        return getFileInputStream(bucket.getName(), key);
    }

    public static InputStream getFileInputStream(String bucketName,String key) {
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
        //System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
        return object.getObjectContent();
    }

    public static File downloadFile(Bucket bucket, String key) throws IOException {
        return downloadFile(bucket.getName(), key);
    }

    public static File downloadFile(String bucketName, String key) throws IOException {
        File file = new File("aws-"+bucketName+"-"+key);
        if (!file.canWrite()) {
            throw new IOException("File is read only");
        }
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

    public static void deleteBucket(String bucketName) {
        s3.deleteBucket(bucketName);
    }

    public static void deleteFile(String bucketName,String key) {
        s3.deleteObject(bucketName,key);
    }

}
