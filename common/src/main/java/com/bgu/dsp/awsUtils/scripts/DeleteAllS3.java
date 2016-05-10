package com.bgu.dsp.awsUtils.scripts;

import com.amazonaws.services.s3.model.Bucket;
import com.bgu.dsp.awsUtils.S3Utils;

/**
 * Created by thinkPAD on 5/6/2016.
 */
public class DeleteAllS3 {
    public static void main(String[] args) {
        for (Bucket b : S3Utils.getBuckets()){
            S3Utils.emptyAnddeleteBucket(b.getName(),true);
        }
    }
}
