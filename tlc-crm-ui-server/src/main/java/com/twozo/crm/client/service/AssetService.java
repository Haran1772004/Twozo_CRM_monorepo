package com.twozo.crm.client.service;

import com.twozo.crm.client.models.Asset;
import com.twozo.crm.client.utils.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class AssetService {

    @Value("${twozo.client.location}")
    private String clientLocation;
    private final Map<String, Asset> assets = new ConcurrentHashMap<>();

    private void load(final String path) {
        assets.computeIfAbsent(path, (f) -> {
            final String separator = path.startsWith("/") ? "" : File.separator;
            final String fileLocation = String.format("%s%s%s", clientLocation, separator, path);

            try {
                final File filePath = new File(fileLocation);

                if (filePath.exists()) {
                    final MediaType mediaType = MediaTypeFactory.getMediaType(fileLocation)
                            .orElseGet(() -> MediaType.APPLICATION_OCTET_STREAM);

                    return new Asset(fileLocation, Files.readAllBytes(filePath.toPath()), mediaType);
                } else {
                    throw new RuntimeException("File not found at path " + fileLocation);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load file: " + fileLocation, e);
            }
        });

    }

    public List<Asset> loadAndGetAssets(final String pattern)  {
        final List<Asset> assetList = new ArrayList<>();

        try {
            final List<Path> files = FileUtils.findFiles(clientLocation, pattern);

            for (final Path file : files) {
                final String fileLocation = file.toString();

                assets.computeIfAbsent(file.toString(), f -> {
                    final MediaType mediaType = MediaTypeFactory.getMediaType(fileLocation)
                            .orElseGet(() -> MediaType.APPLICATION_OCTET_STREAM);

                    try {
                        final Asset asset =  new Asset(fileLocation, Files.readAllBytes(file), mediaType);

                        assetList.add(asset);
                        return asset;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to read files", e);
        }

        return assetList;
    }

    public Asset getAsset(final String path) {
        final String normalizedPath = path.startsWith(File.separator) ? path : String.format("%s%s", File.separator, path);

        if (!assets.containsKey(normalizedPath)) {
            load(normalizedPath);
        }

        return assets.get(normalizedPath);
    }

}
