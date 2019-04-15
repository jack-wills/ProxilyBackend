package org.videoApp.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {
    private static Region region = Regions.getCurrentRegion();
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        if (region != null) {
            GetParameterRequest parameterRequest = new GetParameterRequest().withName("ProxilyEncryptionKey");
            AWSSimpleSystemsManagement ssmclient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
            GetParameterResult parameterResult = ssmclient.getParameter(parameterRequest);
            System.setProperty("ProxilyEncryptionKey", parameterResult.getParameter().getValue());
            if (!new File("rds-ca-2015-root.pem").exists()) {
                try {
                    URL website = new URL("https://s3.amazonaws.com/rds-downloads/rds-ca-2015-root.pem");
                    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                    FileOutputStream fos = new FileOutputStream("rds-ca-2015-root.pem");
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                } catch (IOException e) {
                    LOG.error("Could not retrieve pem file for DB SSL: ", e);
                    throw new IllegalStateException(e.getMessage());
                }
            }
        }
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public FilterRegistrationBean<ProxilyJwtFilter> jwtFilter(){
        FilterRegistrationBean<ProxilyJwtFilter> registrationBean
                = new FilterRegistrationBean<>();

        registrationBean.setFilter(new ProxilyJwtFilter());
        registrationBean.addUrlPatterns("/service/*");

        return registrationBean;
    }
}