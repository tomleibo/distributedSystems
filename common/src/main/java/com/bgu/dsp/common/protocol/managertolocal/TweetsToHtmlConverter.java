package com.bgu.dsp.common.protocol.managertolocal;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.common.protocol.managertolocal.serialize.IllegalSerializedObjectException;
import com.bgu.dsp.common.protocol.managertolocal.serialize.TwitsReader;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;



public class TweetsToHtmlConverter {
    final static Logger log = Logger.getLogger(TweetsToHtmlConverter.class);

    private final String bucketName;
    private final String key;
    private Map<Integer, String> colorMap;

    private final String css = "body {\n" +
            "  margin: 0;\n" +
            "  height: 100%;\n" +
            "  width: 100%;\n" +
            "  font-size: 14px;\n" +
            "  font-family: \"Helvetica Neue\",Helvetica,Arial,Sans-serif;\n" +
            "  font-weight: 300;\n" +
            "  text-align: center;\n" +
            "  cursor: default;\n" +
            "\n" +
            "  -webkit-user-select: none;\n" +
            "  -khtml-user-select: none;\n" +
            "  -moz-user-select: none;\n" +
            "  -o-user-select: none;\n" +
            "  user-select: none;\n" +
            "\n" +
            "  -ms-touch-action: none;\n" +
            "}\n" +
            ".header {\n" +
            "  position: relative;\n" +
            "  height: 100%;\n" +
            "  width: 1200px;\n" +
            "  margin-top: 1cm;"+
            "}\n" +
            ".footer {\n" +
            "  position: fixed;\n" +
            "  bottom: 0;\n" +
            "  left: 0;\n" +
            "  right: 0;\n" +
            "  padding-top: 18px;\n" +
            "  padding-bottom: 2px;\n" +
            "  background: -webkit-gradient(linear, left bottom, left top, from(rgba(255, 255, 255, 1)), to(rgba(255, 255, 255, 0)));\n" +
            "  background: -moz-linear-gradient(bottom, rgba(255, 255, 255, 1), rgba(255, 255, 255, 0));\n" +
            "}"
            +"table { \n" +
            "color: #333;\n" +
            "font-family: Helvetica, Arial, sans-serif;\n" +
            "border-collapse: collapse;\n" +
            "border-spacing: 0; \n" +
            "}\n" +
            "\n" +
            "td, th { \n" +
            "border: 1px solid transparent; /* No more visible border */\n" +
            "height: 30px; \n" +
            "transition: all 0.3s;  /* Simple transition for hover effect */\n" +
            "word-wrap: break-word;"+
            "}\n" +
            "\n" +
            "th {\n" +
            "background: #DFDFDF;  /* Darken header a bit */\n" +
            "font-weight: bold;\n" +
            "}\n" +
            "\n" +
            "td {\n" +
            "background: #FAFAFA;\n" +
            "text-align: left;\n" +
            "}\n" +
            "\n" +
            "/* Cells in even rows (2,4,6...) are one color */ \n" +
            "tr:nth-child(even) td { background: #F1F1F1; }   \n" +
            "\n" +
            "/* Cells in odd rows (1,3,5...) are another (excludes header cells)  */ \n" +
            "tr:nth-child(odd) td { background: #FEFEFE; }  \n" +
            "\n" +
            "tr td:hover { background: #666; color: #FFF; } /* Hover cell effect! */";

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
            long startTime = System.currentTimeMillis();
            convertAndWriteOutputHtml(tmpTweetFile,outputFileName);
            log.debug("Output file writing took "+(System.currentTimeMillis()-startTime)+" milliseconds.");
            tmpTweetFile.delete();
        }
        catch (AmazonS3Exception e) {
            downloadFailed(e, bucketName, key);
        } catch (IOException e) {
            downloadFailed(e, bucketName, key);
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
        String html = "<thead><tr><th>Tweets</td><th>Entities</td></tr></thead>";
        out.println(html);
    }

    private void writeOneTweet(Tweet t, PrintWriter out) {
        log.debug("Writing one tweet to file.");
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
            html.append(getEntityHtml(ent));
        }
        html.append("</td></tr>");
        out.println(html.toString());
    }

	/**
     * Adds a google link for the entity
     */
    private String getEntityHtml(String ent) {
        try {
            int lastIndex = ent.lastIndexOf(':');

            String name = ent.substring(0, lastIndex);
            String rest = ent.substring(lastIndex + 1);;
            String query = name.replace(' ', '+');
            return
                    "<a href=\"http://www.google.com/search?q=" + query + "\">" + name + "</a>:" + rest;
        }
        catch (Exception e){
            log.warn(e);
            return ent;
        }
    }

    private void writeHtmlHeader(PrintWriter out ) {
        String html = "<html><head><title>Distributed Systems Assignment 1</title>" +
                "<style>\n"+css+"\n</style>\n"+
                "</head><body>\n" +
                "<br><h1> Tweet Analysis Output</h1>\n" +
                "<div class=\"header\">\n" +
                "<table>";
        out.println(html);

    }

    private void writeHtmlFooter(PrintWriter out )  {
        String html = "</table></div><div class=\"footer\">Created by Tom Leibovich and Hagai Levi</div></body></html>";
        out.println(html);

    }

    private PrintWriter createFileAppendWriter(String outputFileName) {
        File file = new File(outputFileName);
        if (!file.exists()) {
            try {
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


    private void downloadFailed(Exception e, String bucketName, String key) {
        log.error("download failed. key: " + key + " bucket: " + bucketName, e);
    }

    private void outputFileNotCreated(File file, IOException e) {
        log.error("output file not created", e);
    }
}
