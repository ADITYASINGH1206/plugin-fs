package io.kestra.plugin.fs.vfs;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
class VfsMoveUnitTest {

    @Inject
    private RunContextFactory runContextFactory;

    private StandardFileSystemManager fsm;
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        fsm = new StandardFileSystemManager();
        fsm.init();
        tempDir = Files.createTempDirectory("vfs-move-unit-");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fsm != null) {
            fsm.close();
        }
        if (tempDir != null && Files.exists(tempDir)) {
            try (var s = Files.walk(tempDir)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    @Test
    void shouldMoveFileToNestedTargetWithoutCreatingDirectoryNamedAfterFile() throws Exception {
        var runContext = runContextFactory.of();
        Path source = Files.writeString(tempDir.resolve("source.txt"), "hello world");
        Path targetSubDir = tempDir.resolve("nested/sub");
        Path targetFile = targetSubDir.resolve("target.txt");

        URI fromUri = source.toUri();
        URI toUri = targetFile.toUri();

        Move.Output output = VfsService.move(
            runContext,
            fsm,
            new FileSystemOptions(),
            fromUri,
            toUri,
            true
        );

        assertThat(output).isNotNull();
        assertThat(Files.exists(targetFile)).isTrue();
        assertThat(Files.isDirectory(targetFile)).isFalse();
        assertThat(Files.isRegularFile(targetFile)).isTrue();
        assertThat(Files.readString(targetFile)).isEqualTo("hello world");
        assertThat(Files.exists(source)).isFalse();
    }
}
