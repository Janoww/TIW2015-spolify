package it.polimi.tiw.projects.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class FileData implements AutoCloseable, Serializable {
    private static final long serialVersionUID = 1L;

    private final transient InputStream content;
    private final String filename;
    private final String mimeType;
    private final long size;

    /**
     * Constructs a FileData object.
     *
     * @param content The InputStream providing the file's content.
     * @param filename The name of the file.
     * @param mimeType The MIME type of the file.
     * @param size The size of the file in bytes.
     */
    public FileData(InputStream content, String filename, String mimeType, long size) {
        this.content = content;
        this.filename = filename;
        this.mimeType = mimeType;
        this.size = size;
    }

    public InputStream getContent() {
        return content;
    }

    public String getFilename() {
        return filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSize() {
        return size;
    }

    /**
     * Closes the underlying InputStream. This makes FileData usable in a try-with-resources
     * statement.
     *
     * @throws IOException if an I/O error occurs when closing the stream.
     */
    @Override
    public void close() throws IOException {
        if (content != null) {
            content.close();
        }
    }
}
