package org.folio.dew.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.net.ftp.FTPClient;
import org.folio.dew.config.JacksonConfiguration;
import org.folio.dew.config.properties.FTPProperties;
import org.folio.dew.exceptions.FtpException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SpringBootTest(classes ={JacksonConfiguration.class,
  FTPObjectStorageRepository.class,
  FTPProperties.class,
  FTPClient.class})
class FTPObjectStorageRepositoryTest {

  @Autowired
  private FTPObjectStorageRepository repository;
  @Autowired
  private ObjectFactory<FTPClient> ftpClientFactory;
  @Autowired
  private FTPProperties properties;

  private static FakeFtpServer fakeFtpServer;

  private static final String user_home_dir = "/files";
  private static final String filename = "filename.txt";
  private static final String username_valid = "validUser";
  private static final String password_valid = "letMeIn";
  private static final String password_invalid = "don'tLetMeIn";
  private static final String invalid_uri = "http://localhost";

  private static String uri;

  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSz", Locale.ENGLISH);

  static {
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @BeforeAll
  public static void setup() {
    fakeFtpServer = new FakeFtpServer();
    fakeFtpServer.setServerControlPort(0); // use any free port

    FileSystem fileSystem = new UnixFakeFileSystem();
    fileSystem.add(new DirectoryEntry(user_home_dir));
    fakeFtpServer.setFileSystem(fileSystem);

    UserAccount userAccount = new UserAccount(username_valid, password_valid, user_home_dir);

    fakeFtpServer.addUserAccount(userAccount);

    fakeFtpServer.start();

    uri = "ftp://localhost:" + fakeFtpServer.getServerControlPort() + "/";
    log.info("Mock FTP server running at: " + uri);
  }

  @AfterAll
  public static void teardown() {
    if (fakeFtpServer != null && !fakeFtpServer.isShutdown()) {
      log.info("Shutting down mock FTP server");
      fakeFtpServer.stop();
    }
  }

  @Test
  void testFailedConnect() {
    log.info("=== Test unsuccessful login ===");

    Exception exception = assertThrows(URISyntaxException.class, () -> {
      repository.upload(invalid_uri, username_valid, password_valid, filename, "Some text".getBytes());
    });

    String expectedMessage = "URI should be valid ftp path";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void testFailedLogin() {
    log.info("=== Test unsuccessful login ===");
    assertThrows(FtpException.class, () -> repository.upload(uri, username_valid, password_invalid, filename, "Some text".getBytes()));
  }

  @Test
  void testSuccessfulUpload() {
    log.info("=== Test successful upload ===");

    assertDoesNotThrow(() -> repository.upload(uri, username_valid, password_valid, filename, "Some text".getBytes()));
    assertTrue(fakeFtpServer.getFileSystem().exists(user_home_dir + "/" + filename));
  }

  @Test
  void testFailedUpload() {
    log.info("=== Test unsuccessful upload ===");
    assertThrows(FtpException.class, () -> repository.upload(uri, username_valid, password_valid, "/invalid/path/" + filename, "Some text".getBytes()));
  }
}
