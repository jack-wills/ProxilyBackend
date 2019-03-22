package org.videoApp.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    private static Region region = Regions.getCurrentRegion();
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        if (region != null && !new File("rds-ca-2015-" + region.toString() + ".pem").exists()) {
            try {
                URL website = new URL("https://s3.amazonaws.com/rds-downloads/rds-ca-2015-" +
                        region.toString() + ".pem");
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream("rds-ca-2015-" + region.toString() + ".pem");
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                throw new IllegalStateException(e.getMessage());
            }
        }
        SpringApplication.run(Application.class, args);
    }
}