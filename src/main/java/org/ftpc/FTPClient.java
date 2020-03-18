package org.ftpc;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FTPClient {
  private String downloadPath;
  private Socket ftpClientSock;
  private BufferedReader ftpInputBuffer;
  private BufferedWriter ftpOutputBuffer;

  FTPClient(){
    downloadPath = setDownloadDir();
  }
  FTPClient(String downloadDirPath){
    downloadPath = downloadDirPath;
  }

  public void connect(String host, int port, String user, String password) throws IOException {

    if (ftpClientSock != null) {
      throw new IOException("FTP client already connected. Disconnect first.");
    }

    ftpClientSock = new Socket(host, port);
    ftpInputBuffer = new BufferedReader(new InputStreamReader(ftpClientSock.getInputStream()));
    ftpOutputBuffer = new BufferedWriter(new OutputStreamWriter(ftpClientSock.getOutputStream()));

    String ftpResponse = readResponse();
    if (!ftpResponse.startsWith("220")) {
      throw new IOException("FTP client received unknown response: " + ftpResponse);
    }

    send("USER " + user);
    ftpResponse = readResponse();
    if (!ftpResponse.startsWith("331")) {
      throw new IOException(ftpResponse);
    }

    send("PASS " + password);
    ftpResponse = readResponse();
    if (!ftpResponse.startsWith("230")) {
      throw new IOException(ftpResponse);
    }

  }

  public boolean isConnected() {
    return ftpClientSock.isConnected();
  }

  public void disconnect() throws IOException {
    if (ftpClientSock == null || !ftpClientSock.isConnected() || ftpClientSock.isClosed()) {
      throw new IOException("Client already disconnected!");
    }
    send("QUIT");
    ftpClientSock.close();
    ftpInputBuffer.close();
    ftpOutputBuffer.close();
  }

  public List<String> getListEntries(String folder) throws IOException {
    if (ftpClientSock == null) {
      throw new IOException("Client is not connected!");
    }

    send("PASV");
    String ftpResponse = readResponse();

    send("LIST " + folder);
    if (!ftpResponse.startsWith("227")) {
      throw new IOException(ftpResponse);
    }

    Map<String, Object> ipAndPort = extractIpPort(ftpResponse);
    return handleList((String) ipAndPort.get("ip"), (int) ipAndPort.get("port"));
  }

  public List<String> getListEntriesNLST(String folder) throws IOException {
    if (ftpClientSock == null) {
      throw new IOException("Client is not connected!");
    }

    send("PASV");
    String ftpResponse = readResponse();

    if (!ftpResponse.startsWith("227")) {
      throw new IOException(ftpResponse);
    }

    Map<String, Object> ipAndPort = extractIpPort(ftpResponse);

    send("NLST " + folder);
    return handleList((String) ipAndPort.get("ip"), (int) ipAndPort.get("port"));

  }

  public void upload(File file) throws IOException {
    FileInputStream fileIS = new FileInputStream(file);
    BufferedInputStream inputFileStream = new BufferedInputStream(fileIS);

    send("PASV");
    String ftpResponse = readResponse();

    if (!ftpResponse.startsWith("227")) {
      throw new IOException("Error: " + ftpResponse);
    }

    Map<String, Object> ipAndPort = extractIpPort(ftpResponse);
    Socket dataLink = new Socket((String) ipAndPort.get("ip"), (int) ipAndPort.get("port"));

    send("STOR " + file.getName());
    ftpResponse = readResponse();

    if (!ftpResponse.startsWith("150")) {
      throw new IOException(ftpResponse);
    }

    BufferedOutputStream dataLinkOutStream = new BufferedOutputStream(dataLink.getOutputStream());
    byte[] buffer = new byte[4096];
    int byteRead;
    while ((byteRead = inputFileStream.read()) != -1) {
      dataLinkOutStream.write(buffer, 0, byteRead);
    }

    dataLinkOutStream.flush();
    dataLinkOutStream.close();
    inputFileStream.close();

    ftpResponse = readResponse();
    if (!ftpResponse.startsWith("226")) {
      throw new IOException(
              "Error: " + ftpResponse
      );
    }
  }

  public File download(String fileName) throws IOException {
    send("PASV");

    String addressResponse = readResponse();
    if (!addressResponse.startsWith("227")) {
      throw new IOException("Error: " + addressResponse);
    }

    send("RETR " + fileName);
    String ftpResponse = readResponse();
    if (!ftpResponse.startsWith("150")) {
      throw new IOException(ftpResponse);
    }

    Map<String, Object> ipAndPort = extractIpPort(addressResponse);

    Socket dataLink = new Socket((String) ipAndPort.get("ip"), (int) ipAndPort.get("port"));
    BufferedInputStream dataInputStream = new BufferedInputStream(dataLink.getInputStream());
    int bytesRead;
    FileOutputStream outputFileStream = new FileOutputStream(downloadPath+"/"+fileName);
    byte[] buff = new byte[4096];

    while ((bytesRead = dataInputStream.read(buff)) != -1) {
      outputFileStream.write(buff, 0, bytesRead);
    }
    outputFileStream.flush();
    outputFileStream.close();
    dataInputStream.close();
    dataLink.close();
    ftpResponse = readResponse();
    return new File(downloadPath+"/"+fileName);
  }

  public String getCurrentWorkingDir() throws IOException {
    send("PWD");
    String ftpResponse = readResponse();
    int startIndex = ftpResponse.indexOf("\"");
    int endIndex = ftpResponse.indexOf("\"", startIndex + 1);
    return ftpResponse.substring(startIndex + 1, endIndex);
  }

  public String setWorkingDir(String newWorkingDir) throws IOException {
    send("CWD " + newWorkingDir);
    String ftpResponse = readResponse();
    if (!ftpResponse.startsWith("250")) {
      throw new IOException(ftpResponse);
    }
    int startIndex = ftpResponse.lastIndexOf(' ') + 1;
    return ftpResponse.substring(startIndex, ftpResponse.length() - 1);
  }

  public String makeDirectory(String dirName) throws IOException {
    send("MKD " + dirName);
    String ftpResponse = readResponse();
    if (!ftpResponse.startsWith("257")) {
      throw new IOException(ftpResponse);
    }
    int startIndex = ftpResponse.indexOf("\"");
    int endIndex = ftpResponse.indexOf("\"", startIndex + 1);
    return ftpResponse.substring(startIndex + 1, endIndex);
  }

  public String removeDirectory(String dirName) throws IOException {
    send("RMD " + dirName);
    String ftpResponse = readResponse();
    if (!ftpResponse.startsWith("250")) {
      throw new IOException(ftpResponse);
    }
    int startIndex = ftpResponse.indexOf("\"");
    int endIndex = ftpResponse.indexOf("\"", startIndex + 1);
    return ftpResponse.substring(startIndex + 1, endIndex);
  }

  //  Helper functions for FTP client

  private Map<String, Object> extractIpPort(String address) throws IOException {
    Map<String, Object> ipAndPort = new HashMap<>();
    int firstBracerIndex = address.indexOf('(');
    int lastBracerIndex = address.indexOf(')');

    String dataLink = address.substring(firstBracerIndex + 1, lastBracerIndex);
    StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");

    try {
      ipAndPort.put("ip",
              tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken());
      ipAndPort.put("port",
              Integer.parseInt(tokenizer.nextToken()) * 256 + Integer.parseInt(tokenizer.nextToken()));
    } catch (Exception e) {
      throw new IOException(
              "Error: " + address
      );
    }
    return ipAndPort;
  }

  private List<String> handleList(String ip, int port) throws IOException {
    List<String> list = new LinkedList<>();
    Socket dataChannel = new Socket(ip, port);
    BufferedReader dataIn = new BufferedReader(new InputStreamReader(dataChannel.getInputStream()));
    String line;
    while ((line = dataIn.readLine()) != null) {
      list.add(line);
    }
    dataChannel.close();
    dataIn.close();
    return list;
  }

  private String readResponse() throws IOException {
    if (ftpClientSock == null) {
      throw new IOException("FTP client is not connected");
    }
    return ftpInputBuffer.readLine();
  }

  private void send(String command) throws IOException {
    if (ftpClientSock == null) {
      throw new IOException("FTP client is not connected");
    }
    ftpOutputBuffer.write(command);
    ftpOutputBuffer.newLine();
    ftpOutputBuffer.flush();
  }

  private String setDownloadDir() {
    Path path = Paths.get(App.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
    File downloadDir = new File(path.toAbsolutePath()+"/download");
    if (!downloadDir.isDirectory()) {
      downloadDir.mkdir();
    }
    return downloadDir.getAbsolutePath();
  }
}
