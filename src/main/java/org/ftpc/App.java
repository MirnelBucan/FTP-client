package org.ftpc;

import org.apache.commons.cli.*;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class App {
  public static void main(String[] args) throws InterruptedException {
    System.out.println("Hello world!");
    final String FILE_TEMPLATE = "/tmp%d.txt";
    final String CONTENT = "Hello world!";
    final String USER = "admin";
    final String PASSWORD = "test";
    final String HOST = "localhost";
    final String HOME_DIR = "/";
    final String SUB_DIR = "test";

    FakeFtpServer fakeFtpServer = new FakeFtpServer();
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


    FTPClientService ftpClientService = new FTPClientService("localhost", fakeFtpServer.getServerControlPort(), USER, PASSWORD);
    System.out.println("Uploading ... ");
    ftpClientService.upload(getUploadPaths());

    System.out.println("Downloading ... ");
    ftpClientService.download(getDownloadPaths());

    System.out.println("Listing files: ");
    ftpClientService.listFiles("/");
//    CommandLine cmd = extractArgs(args);

    fakeFtpServer.stop();
  }

  public static List<String> getUploadPaths() {
    List<String> paths = new LinkedList<>();
    ClassLoader classLoader = App.class.getClassLoader();
    paths.add(classLoader.getResource("upload/file1.txt").getPath());
    paths.add(classLoader.getResource("upload/file2.txt").getPath());
    paths.add(classLoader.getResource("upload/file3.txt").getPath());
    return paths;
  }

  public static List<String> getDownloadPaths() {
    List<String> paths = new LinkedList<>();
    paths.add("tmp1.txt");
    paths.add("tmp2.txt");
    return paths;
  }

  public static CommandLine extractArgs(String[] args) {
    Options options = new Options();

    Option host = new Option("h", "host", true, "Option to set host.");
    host.setRequired(true);
    host.setType(String.class);
    options.addOption(host);

    Option port = new Option("p", "port", true, "Option to set port.");
    port.setType(Integer.class);
    port.setRequired(true);
    options.addOption(port);

    Option user = new Option("u", "user", true, "Option to set username.");
    user.setRequired(true);
    user.setType(String.class);
    options.addOption(user);

    Option password = new Option("pw", "password", true, "Option to set password.");
    password.setRequired(true);
    password.setType(String.class);
    options.addOption(password);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("FTP", options);

      System.exit(1);
    }
    return cmd;
  }

}
