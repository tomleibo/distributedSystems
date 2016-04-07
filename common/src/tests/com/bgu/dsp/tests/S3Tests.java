package com.bgu.dsp.tests;

import com.amazonaws.services.s3.model.Bucket;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.bgu.dsp.awsUtils.S3Utils.*;


/**
 * Created by thinkPAD on 3/29/2016.
 */
public class S3Tests {
    @Test
    public void test() throws IOException {
        String bucketName = "randombucketdkfndfkq"+ UUID.randomUUID();
        String prefix = "key";
        String key = prefix +UUID.randomUUID();
        Bucket bucket = createBucket(bucketName);
        List<Bucket> buckets = getBuckets();
        assertTrue(buckets.stream().map(Bucket::getName).collect(Collectors.toList()).contains(bucketName));
        File sampleFile = createSampleFile();
        /*assertTrue(*/uploadFile(bucket, key, sampleFile);
        assertTrue(getFileNames(bucketName, prefix).contains(key));
        //TODO: assert contents of uploaded and downloaded files equals
        deleteFile(bucketName, key);
        assertTrue(getFileNames(bucketName, prefix).isEmpty());
        deleteBucket(bucketName);
        assertFalse(getBuckets().contains(bucket));
    }

    private static File createSampleFile() throws IOException {
        File file = File.createTempFile("aws-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("01234567890112345678901234\n");
        writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
        writer.write("01234567890112345678901234\n");
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.close();

        return file;
    }
}
