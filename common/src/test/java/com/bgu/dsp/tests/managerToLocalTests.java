package com.bgu.dsp.tests;

import com.bgu.dsp.common.protocol.managertolocal.TweetsToHtmlConverter;
import com.bgu.dsp.common.protocol.managertolocal.Tweet;
import org.junit.Test;

import java.io.File;
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
    private List<Tweet> sampleTweetList() {
        List<Tweet> tweets = new ArrayList<>();
        tweets.add(new Tweet("tweet tweet Tom Bash", Arrays.asList(new String[]{"Tom:NAME", "Bash:Location"}), 3, UUID.randomUUID()));
        tweets.add(new Tweet("3489034niu'\"$%#&$&^&%$@5wef948r4934b34f8h", Arrays.asList(new String[]{}), 2, UUID.randomUUID()));
        return tweets;
    }

    private File getSampleTweetFile() throws IOException {
        File temp = File.createTempFile("temp-file-", ".tmp");
        temp.deleteOnExit();
        temp.setWritable(true);
        String data="";
        Files.write(Paths.get(temp.getPath()), data.getBytes(), StandardOpenOption.CREATE);
        return temp;
    }

    @Test
    public void testConvertToHtml() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        String outputFileName = "testHtmlConversion.txt";
        TweetsToHtmlConverter cmd = new TweetsToHtmlConverter("bucketName","keyName");
        Method method = cmd.getClass().getDeclaredMethod("convertAndWriteOutputHtml",File.class,String.class);
        method.setAccessible(true);
        method.invoke(getSampleTweetFile(), outputFileName);
        List<String>  lines = Files.readAllLines(Paths.get(outputFileName), StandardCharsets.UTF_8);
        String expected = "<html><head><title>Distributed Systems Assignment 1</title><link rel=\"stylesheet\" href=\"./css.css\"></head><body><table><tr class=\"color3\"><td>tweet tweet Tom Bash</td><td>Tom:NAME, Bash:Location</td></tr><tr class=\"color2\"><td>3489034niu'&quot;$%#&amp;$&amp;^&amp;%$@5wef948r4934b34f8h</td><td></td></tr></table></body></html>";
        //assertEquals(expected,html);
    }
}
