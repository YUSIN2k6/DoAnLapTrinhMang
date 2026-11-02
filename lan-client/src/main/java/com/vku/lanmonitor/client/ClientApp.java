package com.vku.lanmonitor.client;

import com.vku.lanmonitor.client.service.SocketClientService;
import java.net.InetAddress;

public class ClientApp {

    public static void main(String[] args) {
        // 1. Tự động lấy tên Máy tính để làm định danh (Không cần hiện bảng hỏi nữa)
        String pcName = "Unknown_PC";
        try {
            pcName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            pcName = System.getProperty("user.name", "Client_PC");
        }

        // 2. Khởi tạo kết nối mạng (IP Server truyền vào hoặc mặc định 127.0.0.1)
        // String serverIp = (args.length > 0) ? args[0] : "127.0.0.1";
        String serverIp = "localhost";

        int serverPort = 9999;

        // 3. Khởi động luồng giám sát
        SocketClientService clientService = new SocketClientService(serverIp, serverPort, pcName);
        clientService.start();
    }
}