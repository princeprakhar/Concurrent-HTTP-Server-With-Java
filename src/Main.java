import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

public class Main {
    private static String directory = null;

    public static void main(String[] args) {
        System.out.println("Concurrent HTTP server starting...");
        if (args.length > 1 && args[0].equals("--directory")) {
            directory = args[1];
        } else {
            System.out.println("Usage: java Main --directory <path>");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");
                new Thread(new ClientHandler(clientSocket, directory)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String directory;

    public ClientHandler(Socket socket, String directory) {
        this.clientSocket = socket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try {
            handleRequest(clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handleRequest(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        try {
            // Read the request line
            String requestLine = reader.readLine();
            System.out.println(requestLine);
            if (requestLine == null || requestLine.isEmpty()) {
                sendResponse(output, "HTTP/1.1 400 Bad Request\r\n\r\n", false);
                return;
            }

            String[] requestParts = requestLine.split(" ", 3);
            if (requestParts.length != 3) {
                sendResponse(output, "HTTP/1.1 400 Bad Request\r\n\r\n", false);
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            // Read headers
            String acceptEncoding = "";
            int contentLength = 0;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Accept-Encoding:")) {
                    acceptEncoding = line.substring(17).trim();
                } else if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.substring(16).trim());
                }
            }

            if (method.equals("GET")) {
                handleGetRequest(path, reader, output, acceptEncoding);
            } else if (method.equals("POST")) {
                handlePostRequest(path, input, output, contentLength);
            } else {
                sendResponse(output, "HTTP/1.1 405 Method Not Allowed\r\n\r\n", false);
            }
        } catch (Exception e) {
            System.out.println("Error processing request: " + e.getMessage());
            sendResponse(output, "HTTP/1.1 500 Internal Server Error\r\n\r\n", false);
        }
    }

    private void handleGetRequest(String path, BufferedReader reader, OutputStream output, String acceptEncoding) throws IOException {
        if (path.equals("/")) {
            sendResponse(output, "HTTP/1.1 200 OK\r\n\r\n", false);
        } else if (path.startsWith("/echo/")) {
            String message = path.substring(6);
            sendResponse(output, String.format("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                    message.length(), message), acceptEncoding.contains("gzip"));
        } else if (path.equals("/user-agent")) {
            String userAgent = "";
            String line;
            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("User-Agent:")) {
                    userAgent = line.substring(12).trim();
                    break;
                }
            }
            sendResponse(output, String.format("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                    userAgent.length(), userAgent), acceptEncoding.contains("gzip"));
        } else if (path.startsWith("/files/") && directory != null) {
            String filename = path.substring(7);
            File file = new File(directory, filename);
            if (file.exists() && file.isFile()) {
                byte[] content = Files.readAllBytes(file.toPath());
                sendResponse(output, String.format("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: %d\r\n\r\n",
                        content.length), acceptEncoding.contains("gzip"), content);
            } else {
                sendResponse(output, "HTTP/1.1 404 Not Found\r\n\r\n", false);
            }
        } else {
            sendResponse(output, "HTTP/1.1 404 Not Found\r\n\r\n", false);
        }
    }

    private void handlePostRequest(String path, InputStream input, OutputStream output, int contentLength) throws IOException {
        if (path.startsWith("/files/") && directory != null) {
            String filename = path.substring(7);
            File file = new File(directory, filename);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                int totalBytesRead = 0;
                while (totalBytesRead < contentLength && (bytesRead = input.read(buffer, 0, Math.min(buffer.length, contentLength - totalBytesRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            }

            sendResponse(output, "HTTP/1.1 201 Created\r\n\r\n", false);
        } else {
            sendResponse(output, "HTTP/1.1 404 Not Found\r\n\r\n", false);
        }
    }

    private void sendResponse(OutputStream output, String response, boolean useGzip) throws IOException {
        sendResponse(output, response, useGzip, null);
    }

    private void sendResponse(OutputStream output, String response, boolean useGzip, byte[] content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (useGzip) {
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos)) {
                if (content != null) {
                    gzipOutputStream.write(content);
                } else {
                    gzipOutputStream.write(response.getBytes("UTF-8"));
                }
            }
            byte[] compressedContent = baos.toByteArray();
            String headers = "HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Length: " + compressedContent.length + "\r\n\r\n";
            output.write(headers.getBytes("UTF-8"));
            output.write(compressedContent);
        } else {
            if (content != null) {
                output.write(response.getBytes("UTF-8"));
                output.write(content);
            } else {
                output.write(response.getBytes("UTF-8"));
            }
        }
        output.flush();
    }
}
