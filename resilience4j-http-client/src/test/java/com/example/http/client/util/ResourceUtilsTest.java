package com.example.http.client.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourceUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldResolveClasspathResource() {
        InputStream stream = ResourceUtils.resolveStream("classpath:test-resource.txt");

        Assertions.assertThat(stream).isNotNull();
    }

    @Test
    @SneakyThrows
    void shouldLoadClasspathResourceFromJarPackaging() {
        InputStream stream = ResourceUtils.resolveStream("classpath:test-resource.txt");

        Assertions.assertThat(stream).isNotNull();
        Assertions.assertThat(new String(stream.readAllBytes())).contains("Hello");
    }

    @Test
    void shouldResolveExplicitFilePath(@TempDir Path tempDir) throws Exception {
        Path tempFile = Files.writeString(tempDir.resolve("explicit.txt"), "content");
        String location = "file:" + tempFile.toAbsolutePath();

        InputStream stream = ResourceUtils.resolveStream(location);

        Assertions.assertThat(stream).isNotNull();
    }

    @Test
    void shouldResolvePlainFilePath(@TempDir Path tempDir) throws Exception {
        Path tempFile = Files.writeString(tempDir.resolve("plain.txt"), "content");
        String location = tempFile.toAbsolutePath().toString();

        InputStream stream = ResourceUtils.resolveStream(location);

        Assertions.assertThat(stream).isNotNull();
    }

    @Test
    void shouldThrowExceptionForBlankInput() {
        Assertions.assertThatThrownBy(() -> ResourceUtils.resolveStream(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void shouldThrowExceptionForUnsupportedScheme() {
        String location = "ftp://some-resource.txt";

        Assertions.assertThatThrownBy(() -> ResourceUtils.resolveStream(location))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported resource location");
    }

    @Test
    void shouldThrowExceptionForMissingClasspathResource() {
        String location = "classpath:nonexistent.txt";

        Assertions.assertThatThrownBy(() -> ResourceUtils.resolveStream(location))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Classpath resource not found");
    }

    @Test
    void shouldThrowExceptionForMissingFileResource() {
        String location = "file:/invalid-path.txt";

        Assertions.assertThatThrownBy(() -> ResourceUtils.resolveStream(location))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File resource not found");
    }

    @Test
    void shouldThrowExceptionForMissingPlainFilePath() {
        String location = "/path/does/not/exist.txt";

        Assertions.assertThatThrownBy(() -> ResourceUtils.resolveStream(location))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported resource location");
    }
}
