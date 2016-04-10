package com.bgu.dsp.main;

/**
 * Created by thinkPAD on 4/2/2016.
 */
public class Execute {
    private static LocalEnv env;

    public static void main(String args[]) {
        parseArgs(args);
        LocalMachine local = new LocalMachine();
        env.executor.execute(local);
    }

    public static void parseArgs(String[] args) {
        if (args.length < 3) {
            throw new RuntimeException("usage: inputFileName outputFileName filesToWorkersRate [terminate]");
        }
        env = LocalEnv.build();
        env.inputFileName = args[0];
        env.outputFileName = args[1];
        try {
            env.filesToWorkersRatio = Float.parseFloat(args[2]);
        }
        catch(NumberFormatException e ) {
            System.out.println("third argument should be a number.");
            e.printStackTrace();
            System.exit(1);
        }
        if (args.length > 3) {
            env.terminate = true;
        }
    }
}
