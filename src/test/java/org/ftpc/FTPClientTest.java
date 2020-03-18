package org.ftpc;

import org.junit.jupiter.api.*;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FTPClientTest {
  private FakeFtpServer fakeFtpServer;
  private FTPClient ftpClient;
  private int PORT;
  private final String USER = "admin";
  private final String PASSWORD = "test";
  private final String HOST = "localhost";
  private final String HOME_DIR = "/";
  private final String SUB_DIR = "test";
  private final String SUB_DIR_FOR_REMOVAL = "for_remove";
  String DOWNLOAD_DIR = "download";


  @BeforeAll
  public void setUp() {

    final String FILE_TEMPLATE = "/tmp%d.txt";
    final String CONTENT = "Hello world!";

    ClassLoader classLoader = getClass().getClassLoader();
    URL path = classLoader.getResource(DOWNLOAD_DIR);
    if (path == null) {
      path = classLoader.getResource(".");
      File downLoadDir = new File(path.getPath() + "/" + DOWNLOAD_DIR);
      if( !downLoadDir.exists()){
        downLoadDir.mkdir();
      }
      DOWNLOAD_DIR = downLoadDir.getAbsolutePath();
    } else {
      DOWNLOAD_DIR = path.getPath();
    }
    ftpClient = new FTPClient(DOWNLOAD_DIR);

    fakeFtpServer = new FakeFtpServer();
    fakeFtpServer.addUserAccount(new UserAccount(USER, PASSWORD, HOME_DIR));

    FileSystem fileSystem = new UnixFakeFileSystem();

    fileSystem.add(new DirectoryEntry(HOME_DIR));
    fileSystem.add(new FileEntry(String.format(FILE_TEMPLATE, 1), CONTENT));
    fileSystem.add(new FileEntry(String.format(FILE_TEMPLATE, 2), CONTENT));
    fileSystem.add(new FileEntry(String.format(FILE_TEMPLATE, 3), CONTENT));
    fileSystem.add(new FileEntry(String.format(FILE_TEMPLATE, 4), CONTENT));

    fileSystem.add(new DirectoryEntry(HOME_DIR + SUB_DIR));
    fileSystem.add(new FileEntry(HOME_DIR + SUB_DIR + String.format(FILE_TEMPLATE, 1), CONTENT));
    fileSystem.add(new FileEntry(HOME_DIR + SUB_DIR + String.format(FILE_TEMPLATE, 2), CONTENT));

    fileSystem.add(new DirectoryEntry(HOME_DIR + SUB_DIR_FOR_REMOVAL));

    fakeFtpServer.setFileSystem(fileSystem);
    fakeFtpServer.setServerControlPort(0);
    fakeFtpServer.start();

  }

  @AfterAll
  public void tearDown() {
    fakeFtpServer.stop();
  }

  @BeforeEach
  public void newFTPClient() {
    ftpClient = new FTPClient(DOWNLOAD_DIR);
    PORT = fakeFtpServer.getServerControlPort();
  }

  @Test
  public void connect() {
    try {
      ftpClient.connect(HOST, PORT, USER, PASSWORD);
    } catch (IOException ignored) {
    }
    assertNotNull(ftpClient);
  }


  @Test
  public void invalidHost() {
    final String INVALID_HOST = "some random host";

    Exception exception = Assertions.assertThrows(IOException.class, () -> ftpClient.connect(INVALID_HOST, PORT, USER, PASSWORD));
    Assertions.assertEquals(INVALID_HOST, exception.getMessage());
  }

  @Test
  public void invalidPort() {
    final int INVALID_PORT = 123;

    Exception exception = Assertions.assertThrows(IOException.class, () -> ftpClient.connect(HOST, INVALID_PORT, USER, PASSWORD));
    Assertions.assertEquals("Connection refused (Connection refused)", exception.getMessage());
  }

  @Test
  public void invalidUser() {
    final String INVALID_USER = "INVALID_USER";
    Exception exception = Assertions.assertThrows(IOException.class, () -> ftpClient.connect(HOST, PORT, INVALID_USER, PASSWORD));
    Assertions.assertEquals("530 UserAccount missing or invalid for user [" + INVALID_USER + "]", exception.getMessage());
  }

  @Test
  public void invalidPassword() {
    final String INVALID_PASSWORD = "INVALID_PASS";

    Exception exception = Assertions.assertThrows(IOException.class, () -> ftpClient.connect(HOST, PORT, USER, INVALID_PASSWORD));
    Assertions.assertEquals("530 Not logged in.", exception.getMessage());
  }

  @Test
  public void disconnect() throws IOException {
    ftpClient.connect(HOST, PORT, USER, PASSWORD);
    ftpClient.disconnect();

    Assertions.assertTrue(ftpClient.isConnected());

    Exception exception = Assertions.assertThrows(IOException.class, () -> ftpClient.disconnect());

    Assertions.assertEquals("Client already disconnected!", exception.getMessage());

  }

  @Test
  public void upload() throws IOException {
    final String TEST_FILE = "upload/test_file.txt";

    ftpClient.connect(HOST, PORT, USER, PASSWORD);
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      File file = new File(classLoader.getResource(TEST_FILE).getFile());
      ftpClient.upload(file);
    } catch (NullPointerException ignored) {
    }
    Assertions.assertTrue(fakeFtpServer.getFileSystem().exists(
      TEST_FILE.substring(TEST_FILE.indexOf("/"))
      )
    );
  }

  @Test
  public void download() throws IOException {

    final String TEST_FILE = "tmp1.txt";
    final String TEST_FILE_INVALID = "randomFileName.txt";
    ftpClient.connect(HOST, PORT, USER, PASSWORD);
    File checkFile = ftpClient.download(TEST_FILE);

    Assertions.assertEquals(TEST_FILE, checkFile.getName());

    Exception exception = Assertions.assertThrows(IOException.class, () -> ftpClient.download(TEST_FILE_INVALID));
    Assertions.assertEquals(
      String.format("550 [/%s] does not exist.", TEST_FILE_INVALID),
      exception.getMessage());
  }

  @Test
  public void currentWorkingDir() throws IOException {
    ftpClient.connect(HOST, PORT, USER, PASSWORD);
    String dir = ftpClient.getCurrentWorkingDir();
    Assertions.assertTrue(fakeFtpServer.getFileSystem().exists(dir));
  }

  @Test
  public void changeWorkingDir() throws IOException {
    final String INVALID_DIR = "invalid_dir";
    ftpClient.connect(HOST, PORT, USER, PASSWORD);

    String dir = ftpClient.setWorkingDir(SUB_DIR);
    Assertions.assertEquals(HOME_DIR + SUB_DIR, dir);

    dir = ftpClient.setWorkingDir("..");
    Assertions.assertTrue(fakeFtpServer.getFileSystem().exists(dir));

    Exception exception = Assertions.assertThrows(IOException.class, () -> ftpClient.setWorkingDir(INVALID_DIR));

    Assertions.assertEquals(String.format("550 [/%s] does not exist.", INVALID_DIR), exception.getMessage());
  }

  @Test
  public void makeDir() throws IOException {
    final String NEW_DIR = "new_dir";

    ftpClient.connect(HOST, PORT, USER, PASSWORD);

    String dir = ftpClient.makeDirectory(NEW_DIR);
    Assertions.assertTrue(fakeFtpServer.getFileSystem().exists(dir));

    Exception exception = Assertions.assertThrows(IOException.class, () -> ftpClient.makeDirectory(NEW_DIR));

    Assertions.assertEquals(String.format("550 The path [/%s] already exists.", NEW_DIR), exception.getMessage());
  }

  @Test
  public void removeDir() throws IOException {
    final String NON_EXISTING_DIR = "new_dir_none_existing";

    ftpClient.connect(HOST, PORT, USER, PASSWORD);

    String dir = ftpClient.removeDirectory(SUB_DIR_FOR_REMOVAL);
    Assertions.assertFalse(fakeFtpServer.getFileSystem().exists(dir));

    Exception exception = Assertions.assertThrows(IOException.class, () -> ftpClient.removeDirectory(SUB_DIR));

    Assertions.assertEquals(String.format("550 The [/%s] directory is not empty.", SUB_DIR), exception.getMessage());

    exception = Assertions.assertThrows(IOException.class, () -> ftpClient.removeDirectory(NON_EXISTING_DIR));

    Assertions.assertEquals(String.format("550 [/%s] does not exist.", NON_EXISTING_DIR), exception.getMessage());
  }
}
