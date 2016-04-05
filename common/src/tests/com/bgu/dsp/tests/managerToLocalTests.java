package com.bgu.dsp.tests;

import com.bgu.dsp.common.protocol.managertolocal.NewOutputFileCommand;
import com.bgu.dsp.common.protocol.managertolocal.Tweet;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class managerToLocalTests {
    @Test
    public void testParseFileIntoTweetList() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        NewOutputFileCommand cmd = new NewOutputFileCommand("bucketName","keyName");

        Method method = cmd.getClass().getDeclaredMethod("parseFileIntoTweetList", File.class);
        method.setAccessible(true);
        List<Tweet> tweets = (List<Tweet>) method.invoke(cmd, getSampleTweetFile());
        assertEquals(tweets.get(0), sampleTweetList().get(0));
        assertEquals(tweets.get(1), sampleTweetList().get(1));
    }

    private List<Tweet> sampleTweetList() {
        List<Tweet> tweets = new ArrayList<>();
        tweets.add(new Tweet("tweet tweet Tom Bash", Arrays.asList(new String[] {"Tom:NAME","Bash:Location"}),3));
        tweets.add(new Tweet("3489034niu'\"$%#&$&^&%$@5wef948r4934b34f8h", Arrays.asList(new String[] {}),2));
        return tweets;
    }

    private File getSampleTweetFile() throws IOException {
        File temp = File.createTempFile("temp-file-", ".tmp");
        temp.deleteOnExit();
        temp.setWritable(true);
        String data="2\0 tweet tweet Tom Bash\0 [Tom:NAME,Bash:Location]\0 3\0\n" +
                "0\0 3489034niu'\"$%#&$&^&%$@5wef948r4934b34f8h\0 []\0 2\0";

        //* per tweet: numberOfEnts \0 tweet \0 (entities) [name:TYPE,...] \0 (sentiment) int(0-4) \0
        Files.write(Paths.get(temp.getPath()), data.getBytes(), StandardOpenOption.CREATE);
        return temp;
    }
}
