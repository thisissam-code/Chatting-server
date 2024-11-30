import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Serveur démarré...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Entrez votre nom :");
            clientName = in.readLine();
            Server.broadcast(clientName + " a rejoint le chat", this);

            String message;
            while ((message = in.readLine()) != null) {
                Server.broadcast(clientName + ": " + message, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Server.removeClient(this);
            Server.broadcast(clientName + " a quitté le chat", this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
