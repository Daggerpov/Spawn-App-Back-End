package com.danielagapov.spawn.Config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("!test")
public class S3Config {

    @Bean
    public S3Client s3Client() {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID_");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY_");

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        if (accessKey == null) accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
        if (secretKey == null) secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");


        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        return S3Client.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
