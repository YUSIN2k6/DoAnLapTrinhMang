package com.vku.lanmonitor.server.model;

import lombok.Data;

@Data
public class ClientSession {
    private String id; // ID kết nối (VD: 192.168.1.5:54321)
    private String ipAddress; // IP của sinh viên
    private String pcName; // Mã Sinh Viên (VD: 24ITB217)

    public ClientSession(String id, String ipAddress, String pcName) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.pcName = pcName;
    }
}