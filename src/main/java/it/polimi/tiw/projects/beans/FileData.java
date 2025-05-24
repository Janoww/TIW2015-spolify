package it.polimi.tiw.projects.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.Tika;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FileData implements AutoCloseable, Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    private final transient InputStream content;
    @NotBlank
    private final String filename;
    @NotBlank
    private final String mimeType;
    @Min(0)
    private final long size;

    /**
     * Constructs a FileData object.
     *
     * @param content  The InputStream providing the file's content.
     * @param filename The name of the file.
     * @param mimeType The MIME type of the file.
     * @param size     The size of the file in bytes.
     */
    public FileData(@NotNull InputStream content, @NotBlank String filename, @NotBlank String mimeType,
            @Min(0) long size) {
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
     * Creates a FileData object from a given file path. This method handles opening
     * the InputStream, detecting MIME type, and getting file size. It ensures the
     * InputStream is closed if any error occurs before FileData takes ownership.
     *
     * @param filePath         The path to the file.
     * @param originalFilename The original name of the file.
     * @return A new FileData instance.
     * @throws IOException      if an I/O error occurs during file operations or
     *                          stream handling.
     * @throws RuntimeException if other unexpected errors occur (e.g., from Tika).
     */
    public static FileData createFromFile(Path filePath, String originalFilename) throws IOException {
        InputStream stream = Files.newInputStream(filePath);
        try {
            String mimeType = new Tika().detect(filePath);
            long size = Files.size(filePath);

            return new FileData(stream, originalFilename, mimeType, size);
        } catch (IOException t) {
            try {
                stream.close();
            } catch (IOException closeException) {
                t.addSuppressed(closeException);
            }
            throw t;
        }
    }

    /**
     * Closes the underlying InputStream. This makes FileData usable in a
     * try-with-resources statement.
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
