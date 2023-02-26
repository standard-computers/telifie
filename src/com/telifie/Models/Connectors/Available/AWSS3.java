package com.telifie.Models.Connectors.Available;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.telifie.Models.Connectors.Connector;

public class AWSS3 {

    private AmazonS3 doBuckets;
    private final Connector connector;

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

    public boolean upload(String director, String content){
        director = (director.startsWith("/") ? director.substring(1) : director);
        try {

            PutObjectResult result = doBuckets.putObject(this.connector.getEndpoints().get(0).getDescription(), director, content);
            return result != null;
        } catch (AmazonServiceException e) {

            return false;
        }
    }
}
