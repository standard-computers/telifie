package com.telifie.Models.Utilities;

import org.apache.commons.io.FilenameUtils;
import java.io.File;

public class Asset {

    private String name, path;
    private final String fileType;
    private final long lastModified, size;
    private Library assets;

    public Asset(File file) {
        this.name = file.getName();
        this.path = file.getPath();
        this.fileType = FilenameUtils.getExtension(this.getPath());
        this.lastModified = file.lastModified();
        this.size = file.length();
        this.assets = new Library();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Library getAssets() {
        return assets;
    }

    public void setAssets(Library assets) {
        this.assets = assets;
    }

    public String toJSON() {
        return "{" +
                "\"name\": \"" + name + '\"' +
                ", \"path\": \"" + path + '\"' +
                ", \"file_type\": \"" + fileType + "\"" +
                ", \"last_modified\": " + lastModified +
                ", \"size\": " + size +
                ", \"assets\": [" + assets.toJSON() + "]" +
                '}';
    }
}
