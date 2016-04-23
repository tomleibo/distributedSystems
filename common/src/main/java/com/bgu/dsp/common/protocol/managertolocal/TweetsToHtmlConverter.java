package com.bgu.dsp.common.protocol.managertolocal;

import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.common.protocol.managertolocal.serialize.IllegalSerializedObjectException;
import com.bgu.dsp.common.protocol.managertolocal.serialize.TwitsReader;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import java.io.*;
import java.util.*;

import static j2html.TagCreator.*;


public class TweetsToHtmlConverter {
    final static Logger log = Logger.getLogger(TweetsToHtmlConverter.class);

    private final String bucketName;
    private final String key;
    private Map<Integer, String> colorMap;

    public TweetsToHtmlConverter(String bucketName, String key) {
        this.bucketName=bucketName;
        this.key=key;
        colorMap = new HashMap<>();
        colorMap.put(0, "darkred");
        colorMap.put(1, "red");
        colorMap.put(2, "black");
        colorMap.put(3, "lightgreen");
        colorMap.put(4, "darkgreen");
    }

    public void execute(String outputFileName) {
        try {
            File tmpTweetFile = S3Utils.downloadFile(bucketName, key);
            convertAndWriteOutputHtml(tmpTweetFile,outputFileName);
            tmpTweetFile.delete();
        }
        catch (IOException e) {
            downloadFailed(e);
        }
    }

    private void convertAndWriteOutputHtml(File inFile, String outputFileName) {
        PrintWriter out = createFileAppendWriter(outputFileName);
        writeHtmlHeader(out);
        writeTableHeader(out);
        try (TwitsReader reader = new TwitsReader(inFile.getPath())) {
            reader.init();
            while (true) {
               Tweet t = reader.read();
                writeOneTweet(t,out);
            }
        }
        catch (EOFException e) {
            //everything is alright
        }
        catch (IOException e) {
            log.error("file read failed.",e);
        }
        catch (ClassNotFoundException e) {
            log.error("class not found.", e);
        }
        catch (IllegalSerializedObjectException e) {
            log.error("illegal serialized object.", e);
        }
        finally {
            writeHtmlFooter(out);
            out.close();
        }
    }

    private void writeTableHeader(PrintWriter out) {
        String html = "<tr><th>Tweets</td><th>Entities</td></tr>";
        writeToWriter(html,out);
    }

    private void writeOneTweet(Tweet t, PrintWriter out) {
        StringBuilder html = new StringBuilder();
        html.append("<tr style=\"color: ");
        html.append(colorMap.get(Integer.valueOf(t.sentiment)));
        html.append(";\"><td>");
        html.append(t.tweet);
        html.append("</td><td>");
        String prefix="";
        for (String ent : t.entities) {
            html.append(prefix);
            prefix=", ";
            html.append(ent);
        }
        html.append("</td></tr>");
        writeToWriter(html.toString(),out);
    }

    private void writeHtmlHeader(PrintWriter out ) {
        String html = "<html><head><title>Distributed Systems Assignment 1</title><link rel=\"stylesheet\" href=\"./css.css\"></head><body><table>";
        writeToWriter(html,out);

    }

    private void writeHtmlFooter(PrintWriter out )  {
        String html = "</table></body></html>";
        writeToWriter(html,out);

    }

    private PrintWriter createFileAppendWriter(String outputFileName) {
        File file = new File(outputFileName);
        if (!file.exists()) {
            try {
                //file.mkDirs()
                file.createNewFile();
            }
            catch (IOException e) {
                outputFileNotCreated(file,e);
            }
        }
        try  {
            FileWriter fw = new FileWriter(outputFileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            return out;
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
        return null;
    }

    private void writeToWriter(String html, PrintWriter out) {
        out.println(html);
    }


    private void downloadFailed(IOException e) {
        log.error("download failed.");
    }

    private void outputFileNotCreated(File file, IOException e) {
        log.error("output file not created", e);
    }
}
