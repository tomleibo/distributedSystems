package com.bgu.dsp.awsUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class S3Utils {
    private static AmazonS3Client s3;
    final static Logger logger = Logger.getLogger(S3Utils.class);

    static {
        init();
    }

    public static void init() {

        if ("DEV-LOCAL".equals(System.getenv("DSP_MODE")) || "DEV-LOCAL".equals(System.getenv("DSP_MODE_S3"))) {
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
        else if ("DEV".equals(System.getenv("DSP_MODE")) || "DEV".equals(System.getenv("DSP_MODE_S3"))) {
            logger.info("Using production S3 with local credentials");
            AWSCredentials credentials = Utils.getAwsCredentials();
            s3 = new AmazonS3Client(credentials);
            s3.setRegion(Utils.region);
        }
        else {
            logger.info("Using production S3");
            // TODO shouldn't we set region?
            s3 = new AmazonS3Client(new InstanceProfileCredentialsProvider());
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

    public static void emptyAnddeleteBucket(String bucketName,boolean justEmpty) {
        try {
            System.out.println("Deleting S3 bucket: " + bucketName);
            ObjectListing objectListing = s3.listObjects(bucketName);

            while (true) {
                for (Iterator<?> iterator = objectListing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                    S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                    s3.deleteObject(bucketName, objectSummary.getKey());
                }

                if (objectListing.isTruncated()) {
                    objectListing = s3.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }
            VersionListing list = s3.listVersions(new ListVersionsRequest().withBucketName(bucketName));
            for ( Iterator<?> iterator = list.getVersionSummaries().iterator(); iterator.hasNext(); ) {
                S3VersionSummary s = (S3VersionSummary)iterator.next();
                s3.deleteVersion(bucketName, s.getKey(), s.getVersionId());
            }
            if (!justEmpty) {
                s3.deleteBucket(bucketName);
            }


        } catch (AmazonServiceException ase) {
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Error Message: " + ace.getMessage());
        }

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
        } catch (FileNotFoundException | AmazonClientException e) {
            logger.error(e);
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
        File tmpTweetFile = new File("aws-"+bucketName+"-"+key);
        tmpTweetFile.setWritable(true);
        InputStream fileInputStream = getFileInputStream(bucketName, key);
        FileOutputStream fileOutputStream = new FileOutputStream(tmpTweetFile);

        int read;
        byte[] bytes = new byte[1024];

        while ((read = fileInputStream.read(bytes)) != -1) {
            fileOutputStream.write(bytes, 0, read);
        }

        return tmpTweetFile;
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

    public static List<String> deleteAllS3() {
        List<String> deletedList = new ArrayList<>();
        List<String> exclude = new ArrayList<>();
        exclude.add("dsp-jars");
        List<String> justEmpty = new ArrayList<>();
        justEmpty.add(Utils.LOCAL_TO_MANAGER_BUCKET_NAME);
        justEmpty.add(Utils.MANAGER_TO_LOCAL_BUCKET_NAME);



        for (Bucket b : S3Utils.getBuckets()){
            String bucketName = b.getName();
            if (!exclude.contains(bucketName)) {
                if (justEmpty.contains(bucketName)) {
                    S3Utils.emptyAnddeleteBucket(bucketName,true);
                }
                else{
                    S3Utils.emptyAnddeleteBucket(bucketName,false);
                }
                deletedList.add(bucketName);
            }
        }
        return deletedList;
    }
}
