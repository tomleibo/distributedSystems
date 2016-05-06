package com.bgu.dsp.tests;

import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.common.protocol.managertolocal.TweetsToHtmlConverter;
import com.bgu.dsp.common.protocol.managertolocal.Tweet;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;


public class managerToLocalTests {

    public static final String BUCKET_NAME = "bucketname"+UUID.randomUUID();
    public static final String KEY_NAME = "keyName";
    public static final String FILE_NAME = "HtmlConverterTestFile.txt";

    /*private List<String> sampleStringList() {
        List<String> Strings = new ArrayList<>();
        for (int i=0; i<100; i++) {
            Strings.add(new String("tweet tweet Tom Bash");
        }
        return tweets;
    }*/

    private File getTempFile() throws IOException {
        File temp = File.createTempFile("temp-file-", ".tmp");
        temp.deleteOnExit();
        temp.setWritable(true);
        return temp;
    }

    private void uploadSampleFile() {
        File file = Paths.get("src","test","res", FILE_NAME).toFile();
        if (file.exists()) {
            S3Utils.createBucket(BUCKET_NAME);
            S3Utils.uploadFile(BUCKET_NAME,KEY_NAME,file);
        }
        else {
            throw new RuntimeException("file not found.");
        }
    }

    private void deleteTempFile() {
        S3Utils.deleteFile(BUCKET_NAME,KEY_NAME);
        S3Utils.deleteBucket(BUCKET_NAME);
    }

    @Test
    public void testConvertToHtml() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        uploadSampleFile();
        String outputFileName = "testHtmlConversion.html";
        TweetsToHtmlConverter cmd = new TweetsToHtmlConverter(BUCKET_NAME, KEY_NAME);
        Method method = cmd.getClass().getDeclaredMethod("execute",String.class);
        method.setAccessible(true);
        long startTime = System.currentTimeMillis();
        method.invoke(cmd,outputFileName);
        System.out.println((System.currentTimeMillis()-startTime) + " milliseconds\n");
        List<String>  lines = Files.readAllLines(Paths.get(outputFileName), StandardCharsets.UTF_8);
        String expected = "<html><head><title>Distributed Systems Assignment 1</title><link rel=\"stylesheet\" href=\"./css.css\"></head><body><table><tr class=\"color3\"><td>tweet tweet Tom Bash</td><td>Tom:NAME, Bash:Location</td></tr><tr class=\"color2\"><td>3489034niu'&quot;$%#&amp;$&amp;^&amp;%$@5wef948r4934b34f8h</td><td></td></tr></table></body></html>";
        //assertEquals(expected,html);
    }
}
