[![progress-banner](https://backend.codecrafters.io/progress/http-server/8c4ee7f4-6481-4b79-85b1-0b63da07298c)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)


# Concurrent-HTTP-Server-With-Java

This project is a multithreaded HTTP server written in Java. It handles basic HTTP methods such as `GET` and `POST`, serves files, echoes user-agent strings, supports gzip compression, and allows concurrent client connections.

## Features

- **Concurrent Connections**: Handles multiple clients concurrently using threads.
- **File Serving**: Serves files from a specified directory on `GET` requests.
- **File Upload**: Allows file uploads via `POST` requests to a specific directory.
- **User-Agent Echo**: Returns the client's `User-Agent` string when requested.
- **GZIP Compression**: Supports gzip-encoded responses if requested by the client.
- **Echo Endpoint**: Returns an echo of the message passed in the URL.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 8 or later.
  
### Running the Server

1. **Compile the code**:
   ```bash
   javac Main.java
   ```
2. **Run the server**:
   ```bash
    java Main --directory <path_to_directory>
   ```
   Example:
   ```bash
   java Main --directory ./static
   ```
The server will start on port 4221 and serve files from the specified directory.
## **Usage**
- The server listens for requests on port 4221. You can use curl or a web browser to interact with the server.

#### GET Requests
- *Echo a message:*

  ```bash
  curl http://localhost:4221/echo/hello
  ```
     **Response:**
    ` hello `

- *User-Agent:*

    ```bash
    curl http://localhost:4221/user-agent
    ```
   Response will display the User-Agent string sent by the client.

- *File Download:*

    ```bash
    curl http://localhost:4221/files/<filename>
    ```
  Retrieves the file `<filename>`from the server's directory.

#### POST Requests
  - *File Upload:*
    ```bash
    curl -X POST --data-binary @<path_to_file> http://localhost:4221/files/<filename>
    ```
    Uploads a file to the server directory.
#### GZIP Compression
  If the client requests gzip compression, the server will compress the response. To test it:
  ```bash
  curl --header "Accept-Encoding: gzip" http://localhost:4221/echo/hello --compressed
  ```
## **Code Overview**
- **Main Class**
  The `Main` class is the entry point for the server.
  - It starts the server on port 4221.
  - Accepts a directory path via --directory argument for serving files.
  - It listens for incoming client connections and handles them concurrently using threads.
- **ClientHandler Class**
  Handles the individual client connections in separate threads.
  - handleRequest: Reads the incoming request and determines the request method (GET or POST).
  - GET Request: Handles /echo/, /user-agent, and file serving requests.
  - POST Request: Handles file uploads to the specified directory.
- **GZIP Compression**
  - If the client requests gzip encoding (Accept-Encoding: gzip), the server compresses the response before sending it.
  - Gzip is handled using GZIPOutputStream.
### **Example `curl` Commands**
  - GET User-Agent:
      ```bash
      curl -v --header "User-Agent: foobar/1.2.3" http://localhost:4221/user-agent
      ```
  - Download a File:
      ```bash
      curl http://localhost:4221/files/test.txt -O
      ```
  - Upload a File:
    ```bash
    curl -X POST --data-binary @/path/to/local/file.txt http://localhost:4221/files/uploaded.txt
    ```
  - GZIP-encoded Echo:
    ```bash
    curl --header "Accept-Encoding: gzip" http://localhost:4221/echo/hello --compressed
    ```
## **Troubleshooting**
- If you receive a 404 Not Found response, ensure the file exists in the specified directory.
- If a 400 Bad Request is returned, verify that the request method and headers are properly formatted.
- Check server logs for additional information about client connections or errors.

## Author
**[Prakhar Deep](https://github.com/princeprakhar)**  

# **License**
This project is licensed under the ``MIT License``
```go
    This `README.md` file explains the project, how to compile and run the server, how to make requests, and provides examples of using `curl` for testing.
```
