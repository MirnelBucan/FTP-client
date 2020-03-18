package org.ftpc;

import org.apache.commons.cli.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;


public class App {
  public static void main(String[] args) {
    CommandLine cmd = extractArgs(args);
    String user = cmd.getOptionValue("u");
    String password = cmd.getOptionValue("pw");
    String host = cmd.getOptionValue("h");
    int port = Integer.parseInt(cmd.getOptionValue("h"));

    FTPClientService ftpClientService = new FTPClientService(host, port, user, password);
    if (cmd.hasOption("dl")) {
      ftpClientService.setDownloadDir(cmd.getOptionValue("dl"));
    }
//  Menu loop
    menu(ftpClientService, String.format("%s@%s~", user, host));
  }

  public static CommandLine extractArgs(String[] args) {

    Options options = new Options();

    Option hostOpt = new Option("h", "host", true, "Option to set host.");
    hostOpt.setRequired(true);
    hostOpt.setType(String.class);
    options.addOption(hostOpt);

    Option portOpt = new Option("p", "port", true, "Option to set port.");
    portOpt.setType(Integer.class);
    portOpt.setRequired(true);
    options.addOption(portOpt);

    Option userOpt = new Option("u", "user", true, "Option to set username.");
    userOpt.setRequired(true);
    userOpt.setType(String.class);
    options.addOption(userOpt);

    Option passwordOpt = new Option("pw", "password", true, "Option to set password.");
    passwordOpt.setRequired(true);
    passwordOpt.setType(String.class);
    options.addOption(passwordOpt);

    Option downloadDirOpt = new Option("dl", "dwndir", true, "Option to set download dir." +
      "Note: Use of absolute path is a must!");
    downloadDirOpt.setRequired(false);
    downloadDirOpt.setType(String.class);
    options.addOption(downloadDirOpt);

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

  public static void menu(FTPClientService ftpClientService, String usr) {
    Scanner in = new Scanner(System.in);
    String inputLine, cwd = "/";
    while (true) {
      System.out.print(
        String.format("%s%s: ", usr, cwd)
      );
      inputLine = in.nextLine().trim();
      if (inputLine.equals("exit")) {
        break;
      } else if (inputLine.startsWith("upload")) {
        List<String> files = new LinkedList<>();
        StringTokenizer stringTokenizer;
        if (inputLine.length() == "upload".length()) {
          System.out.println("List files: ");
          stringTokenizer = new StringTokenizer(in.nextLine(), " ");
          while (stringTokenizer.hasMoreTokens()) {
            files.add(stringTokenizer.nextToken());
          }
        } else {
          stringTokenizer = new StringTokenizer(inputLine.substring(inputLine.indexOf(" ") + 1));
          while (stringTokenizer.hasMoreTokens()) {
            files.add(stringTokenizer.nextToken());
          }
        }
        ftpClientService.upload(files);
      } else if (inputLine.startsWith("download")) {
        List<String> files = new LinkedList<>();
        StringTokenizer stringTokenizer;
        if (inputLine.length() == "download".length()) {
          System.out.println("List files: ");
          stringTokenizer = new StringTokenizer(in.nextLine(), " ");
          while (stringTokenizer.hasMoreTokens()) {
            files.add(stringTokenizer.nextToken());
          }
        } else {
          stringTokenizer = new StringTokenizer(inputLine.substring(inputLine.indexOf(" ") + 1));
          while (stringTokenizer.hasMoreTokens()) {
            files.add(stringTokenizer.nextToken());
          }
        }
        ftpClientService.download(files);
      } else if (inputLine.startsWith("cwd")) {
        String changeTo = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
        cwd = ftpClientService.cwd(changeTo.equals("cwd") ? "." : changeTo);
      } else if (inputLine.startsWith("pwd")) {
        ftpClientService.pwd();
      } else if (inputLine.startsWith("ls")) {
        String ls = inputLine.substring(inputLine.indexOf(" ") + 1);
        ftpClientService.listFiles(ls.equals("ls") ? "." : ls);
      } else if (inputLine.startsWith("lsn")) {
        String ls = inputLine.substring(inputLine.indexOf(" ") + 1);
        ftpClientService.listFilesNames(ls.equals("ls") ? "." : ls);
      } else if (inputLine.startsWith("help")) {
        System.out.println("Available commands.");
        System.out.println("usage: FTP");
        System.out.println(" upload <args>        Command for listing files for uploading to server");
        System.out.println(" download <args>      Command for listing files for download from server.");
        System.out.println(" cwd <arg>            Command for change working directory");
        System.out.println(" pwd                  Command for getting current working directory");
        System.out.println(" ls <arg>             Command for listing files (detailed) in current working directory");
        System.out.println(" lsn <arg>            Command for listing files (names) in current working directory");
        System.out.println(" help                 Command for listing available commands");
      }
    }
  }
}
