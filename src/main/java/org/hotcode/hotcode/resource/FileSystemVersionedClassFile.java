package org.hotcode.hotcode.resource;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author khotyn 13-6-26 AM11:21
 */
public class FileSystemVersionedClassFile extends VersionedClassFile {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemVersionedClassFile.class);
    /**
     * The class file in the file jdk.
     */
    private File                file;

    public FileSystemVersionedClassFile(File file){
        try {
            this.file = file;
            updateVersion(file.lastModified());
            setClassFile(FileUtils.readFileToByteArray(file));
        } catch (IOException e) {
            logger.error("Failed to load class file " + file.getName() + ".", e);
        }
    }

    @Override
    public boolean changed() {
        return file.lastModified() > getVersion();
    }

    @Override
    public byte[] reloadAndGetClassFile() {
        updateVersion(file.lastModified());
        try {
            setClassFile(FileUtils.readFileToByteArray(file));
            return classFile;
        } catch (IOException e) {
            logger.error("Failed to reload class file " + file.getName() + ".", e);
            return null;
        }
    }
}
