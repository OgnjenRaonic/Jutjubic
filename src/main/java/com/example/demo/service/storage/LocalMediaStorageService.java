package com.example.demo.service.storage;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class LocalMediaStorageService {

    private final StorageProperties props;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public LocalMediaStorageService(StorageProperties props) {
        this.props = props;
    }

    public StoredFile storeThumbnail(MultipartFile thumbnail) throws IOException, TimeoutException {
        String ct = thumbnail.getContentType();
        if (ct == null || !(ct.equals(MediaType.IMAGE_JPEG_VALUE)
                || ct.equals(MediaType.IMAGE_PNG_VALUE)
                || ct.equals("image/webp"))) {
            throw new IllegalArgumentException("Thumbnail mora biti slika (jpg/png/webp).");
        }

        Path dir = Paths.get(props.getBaseDir(), "thumbnails");
        Files.createDirectories(dir);

        String ext = guessExt(ct, "jpg");
        return storeWithTimeout(thumbnail, dir, ext);
    }

    public StoredFile storeVideoMp4(MultipartFile video) throws IOException, TimeoutException {
        String ct = video.getContentType();
        if (ct == null || !ct.equals("video/mp4")) {
            throw new IllegalArgumentException("Video mora biti mp4 (Content-Type video/mp4).");
        }

        // dodatna zaštita: <= 200MB
        long maxBytes = 200L * 1024L * 1024L;
        if (video.getSize() > maxBytes) {
            throw new IllegalArgumentException("Video je veći od 200MB.");
        }

        Path dir = Paths.get(props.getBaseDir(), "videos");
        Files.createDirectories(dir);

        return storeWithTimeout(video, dir, "mp4");
    }

    private StoredFile storeWithTimeout(MultipartFile file, Path targetDir, String ext)
            throws IOException, TimeoutException {

        String filename = Instant.now().toEpochMilli() + "_" + UUID.randomUUID() + "." + ext;

        // pišemo prvo u TEMP (da možemo da obrišemo delimično)
        Path tempDir = Paths.get(props.getBaseDir(), "tmp");
        Files.createDirectories(tempDir);

        Path tempPath = tempDir.resolve(filename + ".part");
        Path finalPath = targetDir.resolve(filename);

        Future<?> future = executor.submit(() -> {
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            future.get(props.getUploadTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            safeDelete(tempPath);
            safeDelete(finalPath);
            throw new TimeoutException("Upload nije završen u predviđenom vremenu.");
        } catch (ExecutionException ee) {
            safeDelete(tempPath);
            safeDelete(finalPath);
            Throwable cause = ee.getCause();
            if (cause instanceof RuntimeException re && re.getCause() instanceof IOException ioe) {
                throw ioe;
            }
            throw new IOException("Greška prilikom snimanja fajla.", ee);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            safeDelete(tempPath);
            safeDelete(finalPath);
            throw new IOException("Upload prekinut.", ie);
        }

        return new StoredFile(finalPath.toString(), file.getContentType(), file.getSize());
    }

    public void deleteIfExists(String path) {
        if (path == null || path.isBlank()) return;
        safeDelete(Paths.get(path));
    }

    private void safeDelete(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (Exception ignored) {}
    }

    private String guessExt(String contentType, String fallback) {
        if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) return "png";
        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) return "jpg";
        if ("image/webp".equals(contentType)) return "webp";
        return fallback;
    }

    public record StoredFile(String path, String contentType, long sizeBytes) {}
}
