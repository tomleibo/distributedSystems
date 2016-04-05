package com.bgu.dsp.common.protocol.managertolocal;

import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.Utils;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;

import java.io.*;
import java.nio.file.Files;
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
            List<Tweet> matrix = parseFileIntoTweetList(file);
            String html = convertToHtml(matrix);
            writeToFile(Utils.OUTPUT_FILENAME,html);
        }
        catch (IOException e) {
            downloadFailed(e);
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
        int cursor = 0;
        for (String line : lines) {
            Tweet t = parseOneTweetFromFile(line, cursor);
            result.add(t);
        }



         /* output file format:
        * per tweet: numberOfEnts \0 tweet \0 (entities) [name:TYPE,...] \0 (sentiment) int(0-4) \0
                */
        return result;
    }

    private Tweet parseOneTweetFromFile(String line, int cursor) {
        Tweet t = new Tweet();
        String[] substrs = line.split("\0");
        t.setTweet(substrs[1]).setSentiment(Integer.parseInt(substrs[3])).setEntities(Arrays.asList(substrs[2].split(",")));
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
            colorMap.put(0,"rgb(255, 223, 186)");
            colorMap.put(1,"rgb(255, 223, 186)");
            colorMap.put(2,"rgb(255, 255, 186)");
            colorMap.put(3,"rgb(186, 255, 201)");
            colorMap.put(4,"rgb(186, 225, 255)");
            ContainerTag tr = tr().with(td(tweet.tweet), td(entityList.toString()));
            tr.setAttribute("style", "{background-color:" + colorMap.get(tweet.sentiment));
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

    private void writeToFile(String outputFilename, String html) {
        File file = new File(outputFilename);
        if (!file.exists()) {
            file.mkdirs();
            try {
                file.createNewFile();
            }
            catch (IOException e) {
                outputFileNotCreated(file,e);
            }
        }
        try (PrintWriter writer = new PrintWriter(outputFilename, "UTF-8")) {
            writer.println(html);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void outputFileNotCreated(File file, IOException e) {
        //TODO
    }


    private void downloadFailed(IOException e) {
        //TODO
    }

    private void fileReadFailed(File file, IOException e) {
        //TODO
    }
}
