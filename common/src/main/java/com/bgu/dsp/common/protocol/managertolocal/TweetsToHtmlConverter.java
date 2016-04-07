package com.bgu.dsp.common.protocol.managertolocal;

import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.Utils;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static j2html.TagCreator.*;


public class TweetsToHtmlConverter {
    private static final String ERROR_MESSAGE = "download failed";
    private final String bucketName;
    private final String key;

    public TweetsToHtmlConverter(String bucketName, String key) {
        this.bucketName=bucketName;
        this.key=key;
    }

    public String convert() {
        try {
            File file = S3Utils.downloadFile(bucketName, key);
            List<Tweet> tweetList = parseFileIntoTweetList(file);
            return convertToHtml(tweetList);
        }
        catch (IOException e) {
            downloadFailed(e);
            return ERROR_MESSAGE;
        }
    }

    private List<Tweet> parseFileIntoTweetList(File file) {
        List<Tweet> result = new ArrayList<>();
        final int DELIMITER = 0;

        List<String> lines=null;
        try {
            lines = Files.readAllLines(file.toPath());
        }
        catch (IOException e) {
            fileReadFailed(file,e);
        }
        for (String line : lines) {
            Tweet t = parseOneTweet(line);
            result.add(t);
        }
        return result;
    }

    private Tweet parseOneTweet(String line) {
        Tweet t = new Tweet();
        String[] substrs = line.split("\0");
        String ents = substrs[2].substring(2,substrs[2].length()-1);
        t.setTweet(substrs[1].substring(1));
        t.setSentiment(Integer.parseInt(substrs[3].substring(1)));
        t.setEntities(Arrays.asList(ents.split(",")));
        return t;
    }

    private String convertToHtml(List<Tweet> matrix) {
        List<Tag> rows = new ArrayList<>();
        for (Tweet tweet : matrix) {
            StringBuilder entityList = new StringBuilder();
            String prefix="";
            for (String s : tweet.entities) {
                entityList.append(prefix);
                prefix=", ";
                entityList.append(s);
            }
            Map<Integer,String> colorMap = new HashMap<>();
            colorMap.put(0,"color0");
            colorMap.put(1, "color1");
            colorMap.put(2, "color2");
            colorMap.put(3, "color3");
            colorMap.put(4, "color4");
            ContainerTag tr = tr().with(td(tweet.tweet), td(entityList.toString()));
            tr.setAttribute("class",colorMap.get(tweet.sentiment));
            rows.add(tr);
        }


        String html = html().with(
                head().with(
                        title("Distributed Systems Assignment 1"),
                        link().withRel("stylesheet").withHref("./css.css")
                ),
                body().with(
                        table().with(rows)
                )
        ).render();
        return html;
    }



    private void downloadFailed(IOException e) {
        //TODO
    }

    private void fileReadFailed(File file, IOException e) {
        //TODO
    }
}
