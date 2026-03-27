import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

// TODO Hook the API up to basic public functionality (Catalog info)
// TODO Hook the API up to private functionality (User CoursePlan reading and writing)
public class APIServer {

    public static void main(String[] args) throws IOException {
    	
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        System.out.println("Server running on http://localhost:" + port);
        
        server.setExecutor(null);
        server.start();
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}