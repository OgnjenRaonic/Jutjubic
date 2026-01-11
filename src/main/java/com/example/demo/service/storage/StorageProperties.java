package com.example.demo.service.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * Base directory, npr ./uploads
     */
    private String baseDir = "./uploads";

    /**
     * Upload timeout u sekundama (za “predugo traje” rollback test)
     */
    private long uploadTimeoutSeconds = 15;

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public long getUploadTimeoutSeconds() {
        return uploadTimeoutSeconds;
    }

    public void setUploadTimeoutSeconds(long uploadTimeoutSeconds) {
        this.uploadTimeoutSeconds = uploadTimeoutSeconds;
    }
}
