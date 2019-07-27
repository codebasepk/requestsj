package pk.codebase.requests;

import java.io.File;

public class HttpUploadProgress {
    public File currentFile;
    public long upload;
    public long total;
    public int currentNumber;
    public int filesCount;

    public HttpUploadProgress(int count) {
        filesCount = count;
    }

    void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }

    void setUpload(long upload) {
        this.upload = upload;
    }

    void setTotal(long total) {
        this.total = total;
    }

    void setCurrentNumber(int currentNumber) {
        this.currentNumber = currentNumber;
    }
}
