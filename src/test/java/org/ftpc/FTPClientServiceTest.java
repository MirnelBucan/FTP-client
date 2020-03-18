package org.ftpc;

import org.junit.jupiter.api.*;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FTPClientServiceTest {
  private FakeFtpServer fakeFtpServer;
  private FTPClientService ftpClientService;
  private int PORT;
  private final String USER = "admin";
  private final String PASSWORD = "test";
  private final String HOST = "localhost";
  private final String HOME_DIR = "/";
  private final String SUB_DIR = "test";
  String DOWNLOAD_DIR = "download";


  @BeforeAll
  void setUp() {
    final String FILE_TEMPLATE = "/tmp%d.txt";
    final String CONTENT = "Hello world!";

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

    fakeFtpServer.setFileSystem(fileSystem);
    fakeFtpServer.setServerControlPort(0);
    fakeFtpServer.start();


    ClassLoader classLoader = getClass().getClassLoader();
    URL path = classLoader.getResource(DOWNLOAD_DIR);
    if (path == null) {
      path = classLoader.getResource(".");
      File downLoadDir = new File(path.getPath() + "/" + DOWNLOAD_DIR);
      if (!downLoadDir.exists()) {
        downLoadDir.mkdir();
      }
      DOWNLOAD_DIR = downLoadDir.getAbsolutePath();
    } else {
      DOWNLOAD_DIR = path.getPath();
    }
    ftpClientService = new FTPClientService(HOST, fakeFtpServer.getServerControlPort(), USER, PASSWORD);
    ftpClientService.setDownloadDir(DOWNLOAD_DIR);
  }

  @AfterAll
  void tearDown() {
    fakeFtpServer.stop();
  }


  @Test
  void upload() {
    ClassLoader classLoader = getClass().getClassLoader();
    String file = "";
    String[] files = new String[]{"/test_file.txt", "/test_file1.txt", "/test_file2.txt"};
    List<String> testFiles = new LinkedList<String>();

    for (String name : files) {
      file = new File(classLoader.getResource("upload" + name).getFile()).getAbsolutePath();
      testFiles.add(file);
    }

    ftpClientService.upload(testFiles);

    for (String name : files) {
      Assertions.assertTrue(fakeFtpServer.getFileSystem().exists(name));
    }
  }

  @Test
  void download() {
    List<String> testFiles = new LinkedList<>();
    testFiles.add("tmp2.txt");
    testFiles.add("tmp3.txt");
    ftpClientService.download(testFiles);

    ClassLoader classLoader = getClass().getClassLoader();
    for (String fileName : testFiles) {
      File file1 = new File(classLoader.getResource("download/" + fileName).getFile());
      Assertions.assertEquals(file1.getName(), fileName);
    }
  }
}