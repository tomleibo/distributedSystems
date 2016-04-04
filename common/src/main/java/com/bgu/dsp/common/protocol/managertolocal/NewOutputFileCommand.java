package com.bgu.dsp.common.protocol.managertolocal;

import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.Utils;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static j2html.TagCreator.*;

/**
 * Created by thinkPAD on 4/4/2016.
 */
public class NewOutputFileCommand implements NewLocalCommand {
    String bucketName;
    String key;

    public NewOutputFileCommand(String bucketName, String key) {
        this.bucketName=bucketName;
        this.key=key;
    }

    @Override
    public void run() {
        try {
            File file = S3Utils.downloadFile(bucketName,key);
            List<Tweet> matrix = parseFileIntoMatrix(file);
            String html = convertToHtml(matrix);
            writeToFile(Utils.OUTPUT_FILENAME,html);
        }
        catch (IOException e) {
            downloadFailed(e);
        }
    }

    private List<Tweet> parseFileIntoMatrix(File file) {
        List<Tweet> result = new ArrayList<>();
        //TODO
        return result;
    }

    private String convertToHtml(List<Tweet> matrix) {
        List<Tag> rows = new ArrayList<>();
        for (Tweet tweet : matrix) {
            StringBuilder entityList = new StringBuilder();
            for (String s : tweet.entities) {
                entityList.append(s);
            }
            Map<Integer,String> colorMap = new HashMap<>();
            colorMap.put(0,"FF0000");
            colorMap.put(1,"993300");
            colorMap.put(2,"7F7F00");
            colorMap.put(3,"339900");
            colorMap.put(4,"00FF00");
            ContainerTag tr = tr().with(td(tweet.tweet), td(entityList.toString()));
            tr.setAttribute("style", "{background-color:#" + colorMap.get(tweet.sentiment));
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

        /* output file format:
        * per tweet: numberOfEnts \0 tweet \0 (entities) [name:TYPE,...] \0 (sentiment) int(0-4) \0
                */
        return html;
    }



    private void writeToFile(String outputFilename, String html) {

    }

    private void downloadFailed(IOException e) {

    }
}
