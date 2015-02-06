package com.redhat.lightblue.rest.crud;

import de.flapdoodle.embed.process.io.IStreamProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileStreamProcessor implements IStreamProcessor {
    private FileOutputStream outputStream;

    public FileStreamProcessor(File file) throws FileNotFoundException {
        outputStream = new FileOutputStream(file);
    }

    @Override
    public void process(String block) {
        try {
            outputStream.write(block.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onProcessed() {
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
