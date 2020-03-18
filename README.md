# FTP-client

Program to for connecting to ftp server with basic functionalities listing, downloading, uploading, etc.

## Getting Started

### Prerequisites

Before running this program make sure u have JDK and maven installed on your computer.

To check if java and maven are setup on your computer, run following commands.
<br>For java run:
```
java --version
```
For maven run:
```
mvn --version
```

### Installing

To install program on your computer, follow steps.

First clone the repository:
```
git clone https://github.com/MirnelBucan/FTP-client.git
```

Next in directory where project is cloned to, run

```
mvn clean install assembly:single
```

This will create new folder `target`, inside it you'll see jar file `FTPClient.jar` .

### Usage

To run tests, go to directory where pom.xml file is located and run:
```
mvn test
```

To run the program:
<br>
Example:
```
java -cp path/to/FTPClient.jar org.currencyconversion.App -h host -p port -u user -pw password
```
Notes:
- After program starts enter `help` for list of available commands.
- If download directory is not specified with `dl`, program will upon first download from ftp server 
create download directory in same location where jar file is located.
- Flags `-h, -p, -u, -pw` are required for program to run. Optional flag `-dl`.

Detailed flags:
```
usage: FTP
 -dl,--dwndir <arg>     Option to set download dir.
                        Note: Use of absolute path is a must!
 -h,--host <arg>        Option to set host.
 -p,--port <arg>        Option to set port.
 -pw,--password <arg>   Option to set password.
 -u,--user <arg>        Option to set username.
```