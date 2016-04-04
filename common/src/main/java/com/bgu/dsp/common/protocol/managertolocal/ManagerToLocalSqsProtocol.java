package com.bgu.dsp.common.protocol.managertolocal;

import com.bgu.dsp.common.protocol.MalformedMessageException;

/**
 * Created by thinkPAD on 4/4/2016.
 */
public class ManagerToLocalSqsProtocol {
    private static final String OUTPUT_FILE_MESSAGE_TYPE = "OUT_FILE";
    private static final int OUTPUT_FILE_ARGC = 2;


    public static String newFileLocationMessage(String bucketName, String keyName) {
        return "{" + OUTPUT_FILE_MESSAGE_TYPE + "}[" + bucketName + "," + keyName + "]";
    }

    public static NewLocalCommand parse(String message) throws MalformedMessageException {
        String command = message.substring(message.indexOf("{") + 1, message.indexOf("}"));

        if (OUTPUT_FILE_MESSAGE_TYPE.equals(command)){
            return parseNewOutputFileMessage(message);
        }

        throw new MalformedMessageException(message);
    }

    private static NewLocalCommand parseNewOutputFileMessage(String message) throws MalformedMessageException {
        String args = message.substring(message.indexOf("[") + 1, message.indexOf("]"));
        String[] argsArr = args.split(",");

        if (argsArr.length != OUTPUT_FILE_ARGC){
            throw new MalformedMessageException(message);
        }

        String bucketName = argsArr[0];
        String key = argsArr[1];

        return new NewOutputFileCommand(bucketName,key);
    }
}
