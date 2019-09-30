package de.adorsys.datasafe.storage.impl.fs;

import de.adorsys.datasafe.types.api.resource.*;
import de.adorsys.datasafe.types.api.shared.BaseMockitoTest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class FileSystemStorageServiceTest extends BaseMockitoTest {

    private static final String FILE = "file";
    private static final String MESSAGE = "hello";

    private FileSystemStorageService storageService;
    private AbsoluteLocation<PrivateResource> root;
    private AbsoluteLocation<PrivateResource> fileWithMsg;
    private Path storageDir;

    @BeforeEach
    void prepare(@TempDir Path dir) {
        this.storageService = new FileSystemStorageService(new Uri(dir.toUri()));
        this.storageDir = dir;
        this.root = new AbsoluteLocation<>(BasePrivateResource.forPrivate(dir.toUri()));
        this.fileWithMsg = new AbsoluteLocation<>(
                BasePrivateResource.forPrivate(storageDir.toUri().resolve(FILE))
        );
    }

    @Test
    void objectExists() {
        createFileWithMessage();
        assertThat(storageService.objectExists(storageService.list(root).findFirst().get())).isTrue();
    }

    @Test
    void listEmpty() {
        Path nonExistingFile = storageDir.resolve(UUID.randomUUID().toString());
        AbsoluteLocation<PrivateResource> nonExistingFileLocation = new AbsoluteLocation<>(BasePrivateResource.forPrivate(nonExistingFile.toUri()));
        assertThat(storageService.list(nonExistingFileLocation).collect(Collectors.toList())).isEmpty();
    }

    @SneakyThrows
    @Test
    void resolveWithMkDirs() {
        Path nonExistingFolder = storageDir.resolve(UUID.randomUUID().toString());
        Path newFile = nonExistingFolder.resolve(UUID.randomUUID().toString());
        AbsoluteLocation<PrivateResource> newFileLocation = new AbsoluteLocation<>(BasePrivateResource.forPrivate(newFile.toUri()));

        try (OutputStream os = storageService.write(WithCallback.noCallback(newFileLocation))) {
            os.write(MESSAGE.getBytes());
        }
        assertThat(storageService.objectExists(newFileLocation)).isTrue();
    }

    @SneakyThrows
    @Test
    void writeWithException() {
        Path realRoot = Paths.get("/");
        Path beforeRoot = realRoot.resolve("..");
        AbsoluteLocation<PrivateResource> newFileLocation = new AbsoluteLocation<>(BasePrivateResource.forPrivate(beforeRoot.toUri()));

        assertThatThrownBy(() -> {
            try (OutputStream os = storageService.write(WithCallback.noCallback(newFileLocation))) {
                os.write(MESSAGE.getBytes());
            }
        });
    }

    @SneakyThrows
    @Test
    void readWithException() {
        Path beforeRoot = Paths.get("..").resolve(UUID.randomUUID().toString());
        AbsoluteLocation<PrivateResource> newFileLocation = new AbsoluteLocation<>(BasePrivateResource.forPrivate(beforeRoot.toUri()));

        assertThatThrownBy(() -> {
            try (InputStream is = storageService.read(newFileLocation)) {
                StringWriter writer = new StringWriter();
                String encoding = StandardCharsets.UTF_8.name();
                IOUtils.copy(is, writer, encoding);
                log.warn("found: " + writer.toString());
            }
        });
    }

    @SneakyThrows
    @Test
    void listDotFilesToo() {
        Path dotFile = storageDir.resolve(".dotfile");
        AbsoluteLocation<PrivateResource> newFileLocation = new AbsoluteLocation<>(BasePrivateResource.forPrivate(dotFile.toUri()));

        assertThat(storageService.list(root).collect(Collectors.toList())).isEmpty();
        try (OutputStream os = storageService.write(WithCallback.noCallback(newFileLocation))) {
            os.write(MESSAGE.getBytes());
        }
        assertThat(storageService.list(root).collect(Collectors.toList())).isNotEmpty();
    }

    @Test
    void list() {
        createFileWithMessage();

        assertThat(storageService.list(root))
                .hasSize(1)
                .extracting(AbsoluteLocation::location)
                .asString().contains(FILE);
    }

    @Test
    void listOnNonExisting() {
        assertThat(storageService.list(root)).isEmpty();
    }

    @Test
    @SneakyThrows
    void read() {
        createFileWithMessage();
        try (InputStream read = storageService.read(fileWithMsg)) {
            assertThat(read).hasContent(MESSAGE);
        }
    }

    @Test
    @SneakyThrows
    void write() {
        try (OutputStream os = storageService.write(WithCallback.noCallback(fileWithMsg))) {
            os.write(MESSAGE.getBytes());
        }

        try (InputStream read = storageService.read(fileWithMsg)) {
            assertThat(read).hasContent(MESSAGE);
        }
    }

    @Test
    @SneakyThrows
    void removeRemovesOnlyFile() {
        createFileWithMessage("in/some.txt", true);
        createFileWithMessage("in/some_other.txt", false);

        storageService.remove(BasePrivateResource.forAbsolutePrivate(storageDir.toUri().resolve("in/some_other.txt")));

        assertThat(Files.walk(storageDir.resolve("in/some.txt"))).hasSize(1);
        assertThrows(NoSuchFileException.class, () -> Files.walk(storageDir.resolve("in/some_other.txt")));
    }

    @Test
    @SneakyThrows
    void removeRecurses() {
        createFileWithMessage("in/some.txt", true);
        createFileWithMessage("in/deeper/some.txt", true);
        createFileWithMessage("in/deeper/some_other.txt", false);
        createFileWithMessage("in/deeper/and_deeper/some_other.txt", true);

        storageService.remove(BasePrivateResource.forAbsolutePrivate(storageDir.toUri().resolve("in")));

        assertThat(Files.walk(storageDir)).containsOnly(storageDir);
    }

    @SneakyThrows
    private void createFileWithMessage() {
        createFileWithMessage(FILE, false);
    }

    @SneakyThrows
    private void createFileWithMessage(String path, boolean mkDirs) {
        Path resolved = storageDir.resolve(path);
        if (mkDirs) {
            resolved.getParent().toFile().mkdirs();
        }
        Files.write(resolved, MESSAGE.getBytes(), StandardOpenOption.CREATE);
    }
}
