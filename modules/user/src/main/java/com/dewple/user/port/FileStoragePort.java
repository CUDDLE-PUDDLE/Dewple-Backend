package com.dewple.user.port;

import java.io.InputStream;

public interface FileStoragePort {

    String upload(String key, InputStream inputStream, long contentLength, String contentType);

    String generateDownloadUrl(String key);

    void delete(String key);
}
