mvn clean package

java -jar target\lan-client-1.0.0-jar-with-dependencies.jar 
 
 http://10.54.144.242:8080/


1. Phân tích tác dụng của từng file phía Client
pom.xml: File cấu hình Maven. Điểm quan trọng nhất ở đây là nó kéo thư viện jna-platform (để giao tiếp sâu với Windows) và sử dụng maven-assembly-plugin để đóng gói toàn bộ code và thư viện vào một file thực thi duy nhất (chính là file lan-client-1.0.0-jar-with-dependencies.jar mà bạn đã chạy thành công lúc nãy).

ClientApp.java (Lớp Khởi động): Lớp chứa hàm main(). Nhiệm vụ của nó là tự động lấy tên thiết bị của sinh viên (qua InetAddress hoặc user.name), lấy IP của Server (được truyền vào qua dòng lệnh, ví dụ 192.168.1.56), và khởi chạy dịch vụ kết nối mạng.

ActiveWindowUtil.java (Công cụ soi màn hình): Sử dụng thư viện JNA để gọi thẳng xuống Windows API (hàm User32.GetForegroundWindow). File này có nhiệm vụ cực kỳ quan trọng: "Bắt" chính xác tên của tab hoặc phần mềm mà sinh viên đang mở ở trên cùng (Active Window).

ScreenCaptureUtil.java (Công cụ chụp lén): Sử dụng java.awt.Robot để chụp lại toàn bộ màn hình máy tính của sinh viên, sau đó nén ảnh dưới định dạng .jpg để tối ưu dung lượng trước khi truyền qua mạng LAN.

SocketClientService.java (Trái tim của Client): Đây là nơi đảm nhiệm toàn bộ giao thức mạng TCP/IP. Nó duy trì kết nối với Server, liên tục gửi dữ liệu (ảnh, cảnh báo) và lắng nghe/thực thi các mệnh lệnh từ Giám thị đưa xuống.

2. Các bước thiết lập và duy trì luồng TCP/IP trên Client
Toàn bộ quá trình giao tiếp mạng được quy định rõ ràng trong file SocketClientService.java theo các bước sau:

Bước 1: Khởi tạo kết nối vật lý (TCP Handshake)

Hệ thống chạy vòng lặp while (true) để đảm bảo phần mềm luôn cố gắng kết nối.

Lệnh socket = new Socket(serverIp, serverPort); chính thức mở kết nối TCP đến IP máy thật (192.168.1.56) tại cổng 9999.

Sau khi bắt tay thành công, luồng đọc (BufferedReader in) và luồng ghi (PrintWriter out) được mở để sẵn sàng truyền dữ liệu.

Bước 2: Gửi gói tin Báo danh (CONNECT)

Ngay khi đường truyền thông suốt, dòng lệnh đầu tiên Client bắn lên Server là: out.println("CONNECT:" + studentId);. Lệnh này khớp hoàn toàn với logic bên Server để Server biết được IP này tương ứng với máy tính tên gì/mã sinh viên nào và cập nhật lên Dashboard.

Bước 3: Khởi chạy luồng giám sát ngầm (Multithreading)

Hệ thống gọi hàm startBehaviorMonitor(). Hàm này tạo ra một luồng chạy song song (ScheduledExecutorService) cứ 1 giây 1 lần sẽ lấy tên cửa sổ sinh viên đang mở.

Nếu tên cửa sổ không nằm trong whitelist (ví dụ sinh viên mở "Facebook" hoặc "Chat GPT"), nó sẽ lập tức bắn một gói tin qua TCP: out.println("SUSPICIOUS:RED|" + activeTitle); để Server báo động.

Bước 4: Chặn và Lắng nghe lệnh từ Giám thị

Cùng lúc với luồng giám sát ngầm, luồng chính của Client sẽ bị chặn lại tại vòng lặp while (isRunning && (line = in.readLine()) != null). Nghĩa là nó luôn luôn chờ Server "nói" gì đó.

Khi đọc được lệnh từ Server (hàm handleCommand), nó sẽ chia nhánh xử lý:

Nhận UPDATE_WHITELIST:...: Cập nhật lại danh sách các phần mềm được phép dùng.

Nhận CAPTURE_SCREEN: Lập tức gọi ScreenCaptureUtil chụp ảnh, mã hóa thành chuỗi Base64 và bắn ngược lại Server qua lệnh out.println("SCREEN:" + base64);.

Nhận ALERT:...: Dùng JOptionPane bật một hộp thoại thông báo màu vàng chói lọi lên giữa màn hình sinh viên để dằn mặt.

Bước 5: Cơ chế Tự phục hồi (Auto-Reconnect)

Nếu Server bị tắt đột ngột hoặc mạng LAN bị đứt, lệnh in.readLine() sẽ văng lỗi (Exception).

Khối catch và finally sẽ hoạt động: dọn dẹp các tài nguyên mạng cũ (cleanup()), cho tiến trình ngủ ngầm 5 giây (Thread.sleep(5000)), và vòng lặp while(true) sẽ tự động quay lại Bước 1 để cố gắng kết nối lại với Server.