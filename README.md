Tạo file `README.md` tại thư mục gốc để hướng dẫn người tải về từ GitHub cách chạy dự án.

```markdown
# LAN Monitor

Hệ thống giám sát và quản lý phòng máy trong mạng LAN. Dự án gồm nhiều module riêng biệt để thực hiện việc điều phối, giám sát, quản lý bài thi và theo dõi hoạt động của các máy trạm trong cùng một mạng nội bộ.

## 1. Cấu trúc dự án

| Module | Mô tả | Công nghệ |
| --- | --- | --- |
| `lan-server` | Trung tâm điều phối (Hub). Nhận kết nối từ `lan-admin`, `lan-student`, `lan-client` và xử lý các lệnh trong mạng LAN. | Spring Boot 4.1.1, Java 21, TCP Socket |
| `lan-admin` | Ứng dụng quản trị giao diện Swing. Dùng để khởi động server, quản lý máy trạm, quản lý đề thi, xem livestream/webcam, gửi thông báo,... | Java Swing, Java 17, Maven |
| `lan-student` | Ứng dụng dành cho sinh viên/học sinh. Dùng để đăng nhập, làm bài thi trắc nghiệm. | Java, Maven |
| `lan-client` | Ứng dụng giám sát phía client (Anti-cheat). Chạy ngầm trên máy trạm để hỗ trợ giám sát theo yêu cầu của Admin. | Java, Maven |
| `lan-launcher` | Trình khởi động tích hợp, giúp khởi động nhanh các thành phần của hệ thống. | Java, Maven |

## 2. Yêu cầu môi trường

Trước khi chạy dự án, cần cài đặt các công cụ sau:

- **JDK**: 
  - `lan-server` yêu cầu **Java 21**
  - `lan-admin`, `lan-student`, `lan-client`, `lan-launcher` yêu cầu **Java 17** trở lên
- **Maven**: Đã có sẵn `mvnw`/`mvnw.cmd` trong từng module nên không bắt buộc cài đặt Maven toàn cục
- **Hệ điều hành**: Ưu tiên chạy trên **Windows** (do dự án có sử dụng một số logic liên quan đến tiến trình/thiết bị cục bộ)

## 3. Cài đặt và build

Mở terminal tại thư mục gốc `Do_An_Mang/` hoặc vào từng module để build.

### Build toàn bộ từng module

```bash
# 1. Build lan-server
cd lan-server
./mvnw.cmd clean package -DskipTests  # Windows
# hoặc ./mvnw clean package -DskipTests  # Linux/macOS

# 2. Build lan-admin
cd ../lan-admin
./mvnw.cmd clean package -DskipTests

# 3. Build lan-student
cd ../lan-student
./mvnw.cmd clean package -DskipTests

# 4. Build lan-client
cd ../lan-client
./mvnw.cmd clean package -DskipTests

# 5. Build lan-launcher
cd ../lan-launcher
./mvnw.cmd clean package -DskipTests
```

> Sau khi build thành công, file `.jar` sẽ nằm trong thư mục `target/` của từng module.

## 4. Cách chạy dự án

Thứ tự khởi động **bắt buộc**: `lan-server` → sau đó mới chạy `lan-admin`, `lan-student`, `lan-client`.

### Bước 1: Khởi động Server

```bash
cd lan-server
# Chạy trực tiếp bằng Maven
./mvnw.cmd spring-boot:run

# Hoặc chạy file jar sau khi build
java -jar target/lan-server-0.0.1-SNAPSHOT.jar
```

Mặc định server sẽ lắng nghe tại cổng `9999`.

### Bước 2: Khởi động Admin

Có 2 cách:

**Cách A: Chạy file jar đã build**

```bash
cd lan-admin
java -jar target/lan-admin-1.0.0-jar-with-dependencies.jar
```

**Cách B: Chạy từ IDE**  
Mở module `lan-admin` bằng IntelliJ IDEA/NetBeans/Eclipse và chạy class `com.vku.lanmonitor.admin.AdminApp`.

Khi mở giao diện Admin, bạn có thể:

- **Khởi động Server trực tiếp từ màn hình đăng nhập/Dashboard** nếu server chưa chạy.
- **Đăng nhập** với thông tin mặc định: `admin` / `123456` (có thể thay đổi tùy theo cấu hình server).

### Bước 3: Khởi động Student và Client

```bash
# lan-student
cd lan-student
java -jar target/lan-student-1.0.0-jar-with-dependencies.jar

# lan-client
cd lan-client
java -jar target/lan-client-1.0.0-jar-with-dependencies.jar
```

Hoặc khởi động đồng loạt từ giao diện `lan-admin` thông qua panel **"APP LAUNCHER"** (nút *"KHỞI ĐỘNG TẤT CẢ"*).

## 5. Cấu hình nhanh

- **Cổng TCP**: `9999` (được sử dụng giữa các client và server)
- **Kết nối từ Admin**: `localhost:9999` (mặc định trên màn hình đăng nhập)
- **Tài khoản Admin mặc định**: `admin` / `123456`

> Nếu chạy trên nhiều máy trong cùng mạng LAN, cần thay `localhost` bằng IP của máy chạy `lan-server` trên các máy trạm.

## 6. Lưu ý khi up lên GitHub

- Các thư mục `target/` đã được build sẽ không cần commit (nên thêm vào `.gitignore`).
- Chỉ cần commit source code, file `pom.xml`, `mvnw`, `mvnw.cmd` và tài liệu hướng dẫn.
- Đảm bảo người tải về cài đúng phiên bản JDK tương ứng với từng module.
- Nếu dùng trên mạng LAN thực tế, cần mở port `9999` trên tường lửa của máy chủ server.
```