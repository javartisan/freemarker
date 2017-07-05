/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Converter {

    public static final String PROPERTY_NAME_SOURCE = "source";
    public static final String PROPERTY_NAME_DESTINATION_DIRECTORY = "destinationDirectory";

    private static final Logger LOG = LoggerFactory.getLogger(Converter.class);

    private File source;
    private File destinationDirectory;
    private ConversionWarnReceiver conversionWarnReceiver = new LoggingWarnReceiver();
    private boolean createDestinationDirectory;
    private boolean executed;
    private Set<File> directoriesKnownToExist = new HashSet<>();

    public File getSource() {
        return source;
    }

    public void setSource(File source) {
        this.source = source != null ? source.getAbsoluteFile() : null;
    }

    public File getDestinationDirectory() {
        return destinationDirectory;
    }

    public void setDestinationDirectory(File destinationDirectory) {
        this.destinationDirectory = destinationDirectory != null ? destinationDirectory.getAbsoluteFile() : null;
    }

    public boolean isCreateDestinationDirectory() {
        return createDestinationDirectory;
    }

    public void setCreateDestinationDirectory(boolean createDestinationDirectory) {
        this.createDestinationDirectory = createDestinationDirectory;
    }

    public ConversionWarnReceiver getConversionWarnReceiver() {
        return conversionWarnReceiver;
    }

    public void setConversionWarnReceiver(ConversionWarnReceiver conversionWarnReceiver) {
        this.conversionWarnReceiver = conversionWarnReceiver;
    }

    public final void execute() throws ConverterException {
        if (executed) {
            throw new IllegalStateException("This converted was already invoked once.");
        }
        executed = true;

        prepare();
        LOG.debug("Source: {}", source);
        LOG.debug("Destination directory: {}", destinationDirectory);

        convertFiles(source, destinationDirectory, true);
    }

    /**
     * Validate properties and prepare data structures and resources that are shared among individual
     * {@link #convertFile(FileConversionContext)} calls.
     */
    protected void prepare() throws ConverterException {
        MissingRequiredPropertyException.check(PROPERTY_NAME_SOURCE, source);
        MissingRequiredPropertyException.check(PROPERTY_NAME_DESTINATION_DIRECTORY, destinationDirectory);

        if (!source.exists()) {
            throw new PropertyValidationException(PROPERTY_NAME_SOURCE, "File or directory doesn't exist: "
                    + source);
        }

        if (destinationDirectory.isFile()) {
            throw new PropertyValidationException(PROPERTY_NAME_DESTINATION_DIRECTORY,
                    "Destination must a be directory, not a file: " + destinationDirectory);
        }
        if (!createDestinationDirectory && !fastIsDirectory(destinationDirectory)) {
            throw new PropertyValidationException(PROPERTY_NAME_DESTINATION_DIRECTORY,
                    "Directory doesn't exist: " + destinationDirectory);
        }
    }

    private void convertFiles(File src, File dstDir, boolean processSrcDirContentOnly) throws ConverterException {
        if (src.isFile()) {
            convertFile(src, dstDir);
        } else if (fastIsDirectory(src)) {
            for (File sourceListItem : src.listFiles()) {
                convertFiles(
                        sourceListItem,
                        processSrcDirContentOnly ? dstDir : new File(dstDir, src.getName()),
                        false);
            }
        } else {
            throw new ConverterException("Source item doesn't exist (not a file, nor a directory): " + src);
        }
    }

    private void convertFile(File src, File dstDir) throws ConverterException {
        InputStream srcStream;
        try {
            srcStream = new FileInputStream(src);
        } catch (IOException e) {
            throw new ConverterException("Failed to open file for reading: " + src, e);
        }
        try {
            LOG.debug("Converting file: {}", src);
            FileConversionContext fileTransCtx = null;
            try {
                conversionWarnReceiver.setSourceFile(src);
                fileTransCtx = new FileConversionContext(srcStream, src, dstDir, conversionWarnReceiver);
                convertFile(fileTransCtx);
            } catch (IOException e) {
                throw new ConverterException("I/O exception while converting " + _StringUtil.jQuote(src) + ".", e);
            } finally {
                conversionWarnReceiver.setSourceFile(null);
                try {
                    if (fileTransCtx != null && fileTransCtx.outputStream != null) {
                        fileTransCtx.outputStream.close();
                    }
                } catch (IOException e) {
                    throw new ConverterException("Failed to close destination file", e);
                }
            }
        } finally {
            try {
                srcStream.close();
            } catch (IOException e) {
                throw new ConverterException("Failed to close file: " + src, e);
            }
        }
    }

    private void ensureDirectoryExists(File dir) throws ConverterException {
        if (dir == null || fastIsDirectory(dir)) {
            return;
        }
        ensureDirectoryExists(dir.getParentFile());
        if (!dir.mkdir()) {
            throw new ConverterException("Failed to create directory: " + dir);
        } else {
            LOG.debug("Directory created: {}", dir);
            directoriesKnownToExist.add(dir);
        }
    }

    /**
     * Only works correctly for directories that won't be deleted during the life of this {@link Converter} object.
     */
    private boolean fastIsDirectory(File dir) {
        if (directoriesKnownToExist.contains(dir)) {
            return true;
        }

        LOG.trace("Checking if is directory: {}", dir);
        boolean exists = dir.isDirectory();

        if (exists) {
            directoriesKnownToExist.add(dir);
        }

        return exists;
    }

    /**
     * Converts a single file. To content of file to convert should be accessed with
     * {@link FileConversionContext#getSourceStream()}. To write the converted file, first you must call
     * {@link FileConversionContext#setDestinationFileName(String)},
     * then {@link FileConversionContext#getDestinationStream()} to start writing the converted file.
     */
    protected abstract void convertFile(FileConversionContext fileTransCtx) throws ConverterException, IOException;

    public class FileConversionContext {

        private final InputStream sourceStream;
        private final File sourceFile;
        private final File dstDir;
        private final ConversionWarnReceiver conversionWarnReceiver;
        private String destinationFileName;
        private OutputStream outputStream;

        public FileConversionContext(
                InputStream sourceStream, File sourceFile, File dstDir, ConversionWarnReceiver conversionWarnReceiver) {
            this.sourceStream = sourceStream;
            this.sourceFile = sourceFile;
            this.dstDir = dstDir;
            this.conversionWarnReceiver = conversionWarnReceiver;
        }

        /**
         * The source file; usually not used; to read the file use {@link #getSourceStream()}.
         */
        public File getSourceFile() {
            return sourceFile;
        }

        public String getSourceFileName() {
            return sourceFile.getName();
        }

        /**
         * Read the content of the source file with this. You need not close this stream in
         * {@link Converter#convertFile(FileConversionContext)}; the {@link Converter} will do that.
         */
        public InputStream getSourceStream() {
            return sourceStream;
        }

        /**
         * Write the content of the destination file with this.  You need not close this stream in
         * s         * {@link Converter#convertFile(FileConversionContext)}; the {@link Converter} will do that.
         */
        public OutputStream getDestinationStream() throws ConverterException {
            if (outputStream == null) {
                if (destinationFileName == null) {
                    throw new IllegalStateException("You must set FileConversionContext.destinationFileName before "
                            + "starting to write the destination file.");
                }

                ensureDirectoryExists(dstDir);
                File dstFile = new File(dstDir, destinationFileName);
                try {
                    outputStream = new FileOutputStream(dstFile);
                } catch (IOException e) {
                    throw new ConverterException("Failed to open file for writing: " + dstFile, e);
                }
            }
            return outputStream;
        }

        public String getDestinationFileName() {
            return destinationFileName;
        }

        /**
         * Sets the name of the file where the output will be written.
         *
         * @param destinationFileName
         *         Can't contain directory name, only the file name.
         */
        public void setDestinationFileName(String destinationFileName) {
            if (outputStream != null) {
                throw new IllegalStateException("The destination file is already opened for writing");
            }
            _NullArgumentException.check("destinationFileName", destinationFileName);
            if (destinationFileName.contains("/") || destinationFileName.contains(File.separator)) {
                throw new IllegalArgumentException(
                        "The destination file name can't contain directory name: " + destinationFileName);
            }
            this.destinationFileName = destinationFileName;
        }

        public ConversionWarnReceiver getConversionWarnReceiver() {
            return conversionWarnReceiver;
        }

    }

}
