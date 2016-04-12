package com.bgu.dsp.common.protocol.managertolocal;

import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.common.protocol.managertolocal.serialize.IllegalSerializedObjectException;
import com.bgu.dsp.common.protocol.managertolocal.serialize.TwitsReader;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static j2html.TagCreator.*;


public class TweetsToHtmlConverter {
    final static Logger log = Logger.getLogger(TweetsToHtmlConverter.class);

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
        try (TwitsReader reader = new TwitsReader(file.getPath())) {
            reader.init();
            while (true) {
               Tweet t = reader.read();
               result.add(t);
            }
        }
        catch (EOFException e) {
            //everything is alright
        }
        catch (IOException e) {
            log.log(Priority.ERROR,"file read failed.",e);
        }
        catch (ClassNotFoundException e) {
            log.log(Priority.ERROR, "class not found.", e);
        }
        catch (IllegalSerializedObjectException e) {
            log.log(Priority.ERROR, "illegal serialized object.", e);
        }
        return result;
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
        log.log(Priority.ERROR,"download failed.");
    }

    private void fileReadFailed(File file, IOException e) {
        log.log(Priority.ERROR,"file read failed.");
    }
}
