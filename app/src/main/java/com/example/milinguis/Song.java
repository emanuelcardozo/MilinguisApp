package com.example.milinguis;

import java.io.File;

public class Song {

    private String name;
    private byte[] bytesFile;

    public Song(String fileName, byte[] bytesFile) {
        this.name = fileName;
        this.bytesFile = bytesFile;
    }

    public String getName() {
        return name;
    }

    public byte[] getBytesFile() {
        return bytesFile;
    }
}
