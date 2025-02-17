package org.ftpc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class FTPClientService {
  private String host;
  private String user;
  private String password;
  private String currentWorkingDir;
  private String downloadDir;
  private int port;

  public FTPClientService(String host, int port, String user, String password) {
    this.host = host;
    this.user = user;
    this.password = password;
    this.port = port;
    currentWorkingDir = "/";
    downloadDir = null;
    System.out.println(this.host +" "+this.user +" "+this.password +" "+this.port );
  }

  public void setDownloadDir(String dir){
    downloadDir = dir;
  }

  public String getDownloadDir(){
    return downloadDir;
  }

  public void pwd() {
    System.out.println("Working dir: " + currentWorkingDir);
  }

  public String cwd(String dir) {
    FTPClient ftpClient = getFTPClient();
    try {
      currentWorkingDir = ftpClient.setWorkingDir(dir);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    return currentWorkingDir;
  }

  public void listFiles(String dir) {
    FTPClient ftpClient = getFTPClient();
    try {
      List<String> files = ftpClient.getListEntries(dir);
      for (String file : files) {
        System.out.println(file);
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }

  public void listFilesNames(String dir) {
    FTPClient ftpClient = getFTPClient();
    try {
      List<String> files = ftpClient.getListEntriesNLST(dir);
      for (String file : files) {
        System.out.println(file);
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }

  public void upload(List<String> filesPaths) {

    System.out.println("Uploading...");
    List<Callable<Void>> tasks;
    AtomicLong totalFilesSize = new AtomicLong();
    AtomicLong totalDuration = new AtomicLong();

    tasks = filesPaths
      .stream()
      .map(filePath -> {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        File file = new File(path.toString());
        return (Callable<Void>) () -> {
          FTPClient ftpClient = getFTPClient();
          try {
            long uploadStart = System.currentTimeMillis();
            ftpClient.upload(file);
            long uploadEnd = System.currentTimeMillis();
            double uploadTime = ((uploadEnd - uploadStart) / 1000.0);
            totalDuration.addAndGet((long) uploadTime);
            totalFilesSize.addAndGet(file.length());
            System.out.println(
              String.format(
                "-File name: %s\n-File size: %s\n-Duration: %.2f s\n-Uploaded file path: %s\n",
                file.getName(), getTotalFileSize(new AtomicLong(file.length()))
                , uploadTime, file.getAbsolutePath()
              )
            );
          } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
          } finally {
            ftpClient.disconnect();
          }
          return null;
        };

      }).collect(Collectors.toList());

    ExecutorService threadPool = Executors.newFixedThreadPool(filesPaths.size());
    long uploadStartTime = System.currentTimeMillis();

    try {
      threadPool.invokeAll(tasks);
      threadPool.shutdown();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }

    long uploadStopTime = System.currentTimeMillis();
    double duration = (uploadStopTime - uploadStartTime) / 1000.0;

    System.out.println(
      String.format(
        "Total upload size: %s\nTotal upload time: %.2f s",
        getTotalFileSize(totalFilesSize), totalDuration.doubleValue()
      )
    );
  }

  public void download(List<String> filesPaths) {
    System.out.println("Downloading...");

    List<Callable<Void>> tasks;
    AtomicLong totalFilesSize = new AtomicLong();
    AtomicLong totalDuration = new AtomicLong();
    tasks = filesPaths
      .stream()
      .map(filePath -> (Callable<Void>) () -> {
        FTPClient ftpClient = getFTPClient();

        try {
          long downloadStart = System.currentTimeMillis();
          File file = ftpClient.download(filePath);
          System.out.println(file.getAbsolutePath());
          System.out.println(file.exists());
          long downloadEnd = System.currentTimeMillis();
          double downloadTime = ((downloadEnd - downloadStart) / 1000.0);
          totalDuration.addAndGet((long) downloadTime);
          totalFilesSize.addAndGet(file.length());

          System.out.println(
            String.format(
              "-File name: %s\n-File size: %s\n-Duration: %.2f s\n-Downloaded file path: %s\n",
              file.getName(), getTotalFileSize(new AtomicLong(file.length())),
              downloadTime, file.getAbsolutePath()
            )
          );
        } catch (IOException e) {
          e.printStackTrace();
          System.err.println(e.getMessage());
        }

        return null;
      }).collect(Collectors.toList());

    ExecutorService threadPool = Executors.newFixedThreadPool(filesPaths.size());
    try {
      threadPool.invokeAll(tasks);
      threadPool.shutdown();
    } catch (InterruptedException e) {
      System.err.println(e.getMessage());
    }

    System.out.println(String.format(
      "Total download size: %s\nTotal download time: %.2f s"
      , getTotalFileSize(totalFilesSize), totalDuration.doubleValue()));
  }

  private FTPClient getFTPClient() {
    FTPClient ftpc = downloadDir == null ? new FTPClient() : new FTPClient(downloadDir);
    try {
      ftpc.connect(host, port, user, password);
      ftpc.setWorkingDir(currentWorkingDir);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    return ftpc;
  }

  private String getTotalFileSize(AtomicLong filesSize) {
    double fileSizeTotal = filesSize.doubleValue();
    String units = "B";
    if (fileSizeTotal > 1024) {
      fileSizeTotal = fileSizeTotal / 1024;
      units = "kB";
    }
    if (fileSizeTotal > 1024) {
      fileSizeTotal = fileSizeTotal / 1024;
      units = "MB";
    }
    if (fileSizeTotal > 1024) {
      fileSizeTotal = fileSizeTotal / 1024;
      units = "GB";
    }

    return String.format("%.2f %s", fileSizeTotal, units);
  }
}
