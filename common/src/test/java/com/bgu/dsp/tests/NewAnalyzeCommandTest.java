package com.bgu.dsp.tests;

import com.bgu.dsp.common.protocol.managertoworker.NewAnalyzeCommand;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class NewAnalyzeCommandTest {
    @Test
    public void testGetTitleFromUrl() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        UUID uuid = UUID.randomUUID();
        String url = "https://twitter.com/realDonaldTrump/status/711388380668493824";
        NewAnalyzeCommand cmd = new NewAnalyzeCommand(uuid,"random name"+uuid.toString(),url);
        Method method = cmd.getClass().getDeclaredMethod("getTitleFromUrl");
        method.setAccessible(true);
        String title = (String) method.invoke(cmd);
        String expected = "Donald J. Trump on Twitter: \"THANK YOU ARIZONA! Get out and #VoteTrump on Tuesday! #AZPrimary #MakeAmericaGreatAgain #Trump2016 https://t.co/5itxkQxrLF\"";
        Assert.assertEquals(expected, title);
    }
}
