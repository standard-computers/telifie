package com.telifie.Models.Connectors.Available;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.telifie.Models.Actions.Out;
import com.telifie.Models.Connectors.Connector;
import com.telifie.Models.Utilities.Tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class AWSS3 {

    private final AmazonS3 doBuckets;
    private final Connector connector;
    private final String workingDirectory = Tool.getWorkingDirectory();

    public AWSS3(Connector connector){

        this.connector = connector;
        AWSCredentialsProvider doCred = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(connector.getClient(),
                        connector.getRefreshToken()));
        doBuckets = AmazonS3ClientBuilder.standard()
                .withCredentials(doCred)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("https://nyc3.digitaloceanspaces.com", "nyc3"))
                .build();
    }

    public boolean upload(String director, byte[] content, boolean isPublic){

        File temps = new File(this.workingDirectory + "temps");
        if(!temps.exists()){
            temps.mkdirs();
        }
        String tempFileName = UUID.randomUUID().toString() + "." + director.split("\\.")[director.split("\\.").length - 1];
        Out.console("Writing file -> " +this.workingDirectory + "temps/" + tempFileName);
        File tempFile = new File(this.workingDirectory + "temps/" + tempFileName);
        FileOutputStream fos = null;
        try {

            fos = new FileOutputStream(tempFile);
            fos.write(content);
            fos.flush();
            fos.close();

            director = (director.startsWith("/") ? director.substring(1) : director);
            try {

                PutObjectResult result;

                if(isPublic){

                    PutObjectRequest putRequest = new PutObjectRequest(this.connector.getEndpoints().get(0).getDescription(), director, tempFile);
                    putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
                    result = doBuckets.putObject(putRequest);
                }else{

                    result = doBuckets.putObject(this.connector.getEndpoints().get(0).getDescription(), director, tempFile);
                }
                tempFile.delete();
                return result != null;
            } catch (AmazonServiceException e) {

                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {

            throw new RuntimeException(e);
        } catch (IOException e) {

            throw new RuntimeException(e);
        }
    }
}
