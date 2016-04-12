package java.com.bgu.dsp.tests;

import com.bgu.dsp.common.protocol.managertolocal.TweetsToHtmlConverter;
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
        TweetsToHtmlConverter cmd = new TweetsToHtmlConverter("bucketName","keyName");
        Method method = cmd.getClass().getDeclaredMethod("parseFileIntoTweetList", File.class);
        method.setAccessible(true);
        List<Tweet> tweets = (List<Tweet>) method.invoke(cmd, getSampleTweetFile());
        assertEquals(tweets.get(0).toString(), sampleTweetList().get(0).toString());
        assertEquals(tweets.get(1).toString(), sampleTweetList().get(1).toString());
    }

    private List<Tweet> sampleTweetList() {
        List<Tweet> tweets = new ArrayList<>();
        tweets.add(new Tweet("tweet tweet Tom Bash", Arrays.asList(new String[]{"Tom:NAME", "Bash:Location"}), 3));
        tweets.add(new Tweet("3489034niu'\"$%#&$&^&%$@5wef948r4934b34f8h", Arrays.asList(new String[]{}), 2));
        return tweets;
    }

    private File getSampleTweetFile() throws IOException {
        File temp = File.createTempFile("temp-file-", ".tmp");
        temp.deleteOnExit();
        temp.setWritable(true);
        String data="2\0 tweet tweet Tom Bash\0 [Tom:NAME,Bash:Location]\0 3\0\n" +
                "0\0 3489034niu'\"$%#&$&^&%$@5wef948r4934b34f8h\0 []\0 2\0";
        Files.write(Paths.get(temp.getPath()), data.getBytes(), StandardOpenOption.CREATE);
        return temp;
    }

    @Test
    public void testConvertToHtml() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TweetsToHtmlConverter cmd = new TweetsToHtmlConverter("bucketName","keyName");
        Method method = cmd.getClass().getDeclaredMethod("convertToHtml",List.class);
        method.setAccessible(true);
        String html = (String) method.invoke(cmd, sampleTweetList());
        String expected = "<html><head><title>Distributed Systems Assignment 1</title><link rel=\"stylesheet\" href=\"./css.css\"></head><body><table><tr class=\"color3\"><td>tweet tweet Tom Bash</td><td>Tom:NAME, Bash:Location</td></tr><tr class=\"color2\"><td>3489034niu'&quot;$%#&amp;$&amp;^&amp;%$@5wef948r4934b34f8h</td><td></td></tr></table></body></html>";
        assertEquals(expected,html);
    }
}
