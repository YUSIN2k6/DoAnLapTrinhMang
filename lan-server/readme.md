1. Kiến trúc mạng và Các giao thức sử dụng
   Hệ thống hoạt động dựa trên hai luồng giao tiếp song song:

Luồng 1: Giám sát máy trạm (Java Sockets - TCP/IP). Giao tiếp giữa ứng dụng Client (chạy ngầm trên máy sinh viên) và Server. Luồng này chuyên nhận luồng ảnh màn hình liên tục và các cảnh báo gian lận.

Luồng 2: Điều khiển thời gian thực (WebSocket - STOMP). Giao tiếp giữa Server và trình duyệt Web (của Giám thị). Server nhận ảnh/cảnh báo từ luồng 1 và ngay lập tức "bắn" sang luồng 2 để hiển thị lên Dashboard.

2. Phân tích lõi kết nối TCP/IP (File TcpServerService.java)
   File TcpServerService.java chính là "trái tim" của hệ thống giám sát. Nó mở một cổng mạng (port) để các máy sinh viên (Client) kết nối vào.

Khởi động Server Socket:

Trong hàm startServer(), Server mở cổng 9999 để đón kết nối:

Java
serverSocket = new ServerSocket(9999);
Server chạy một vòng lặp vô hạn while (isRunning), liên tục gọi serverSocket.accept() để chờ kết nối. Khi có một máy sinh viên kết nối vào, nó sinh ra một Socket mới.

Hệ thống sử dụng ExecutorService threadPool để tạo một luồng (thread) mới tên là ClientHandler nhằm phục vụ riêng cho máy sinh viên đó, giúp Server có thể phục vụ nhiều máy cùng lúc mà không bị đơ.

Xử lý luồng kết nối của từng sinh viên (ClientHandler):

Nhận và Gửi: Mở luồng đọc (BufferedReader in) và luồng ghi (PrintWriter out) để giao tiếp hai chiều.

Quản lý phiên (Session): Tạo một đối tượng ClientSession lưu trữ clientId (IP:Port), ipAddress và pcName (Mã sinh viên), sau đó đưa vào bản đồ activeClients. Server gọi notificationService.notifyClientListUpdated để báo cho Web biết có máy mới kết nối.

Đọc dữ liệu liên tục: Vòng lặp while ((line = in.readLine()) != null) liên tục "lắng nghe" các gói tin text từ Client gửi lên. Các gói tin này được định dạng bằng các "tiền tố" (prefix) để phân loại:

CONNECT:Mã_Sinh_Viên: Client gửi mã sinh viên để cập nhật tên lên hệ thống.

SCREEN:Base64_Ảnh: Client gửi chuỗi ảnh chụp màn hình (Base64). Server sẽ gọi notificationService.sendScreenshotToWeb đẩy ảnh này lên thẳng giao diện Web của giám thị. Nếu giám thị đang bấm nút "Chụp Ảnh", nó sẽ lưu chuỗi Base64 này thành file vật lý trong thư mục images/.

SUSPICIOUS:Mức_độ|Tên_Tab: Khi ứng dụng dưới Client phát hiện sinh viên mở tab/phần mềm không hợp lệ (nằm ngoài Whitelist), nó sẽ gửi cảnh báo lên. Cảnh báo này lại được đẩy qua WebSocket lên Web để nháy đỏ.

Các hàm điều khiển từ xa:

sendCommand(clientId, cmd): Gửi lệnh (như "CAPTURE_SCREEN" hoặc "ALERT:...") xuống một máy trạm cụ thể.

broadcast(cmd): Gửi lệnh (ví dụ: cập nhật Whitelist mới "UPDATE_WHITELIST:...") xuống tất cả các máy trạm đang thi.

3. Phân tích Luồng WebSocket (Giao diện Giám thị)
   Giao diện Web của giám thị (dashboard.html và app.js) không tự động gọi API liên tục để lấy thông tin. Thay vào đó, nó mở một kết nối WebSocket để nhận dữ liệu đẩy từ Server một cách thụ động (Real-time).

Cấu hình trên Server (WebSocketConfig.java):

Khai báo endpoint /ws để Web kết nối vào.

Tạo ra các "kênh phát thanh" (Broker) có tiền tố /topic. Bất kỳ ai đăng ký kênh này sẽ nhận được thông báo.

Dịch vụ đẩy thông báo (RealtimeNotificationService.java):

Cung cấp các hàm để TcpServerService gọi khi có sự kiện:

notifyClientListUpdated: Đẩy vào /topic/clients.

sendScreenshotToWeb: Đẩy vào /topic/screenshot/{clientId}.

sendSuspiciousAlertToWeb: Đẩy vào /topic/alerts.

Kết nối phía Frontend (js/app.js):

Hàm connectWebSocket() kết nối đến /ws thông qua thư viện SockJS và Stomp.

Nó "đăng ký" (subscribe) các kênh tương ứng:

stompClient.subscribe("/topic/clients", ...): Render lại lưới máy tính.

stompClient.subscribe("/topic/alerts", ...): Kích hoạt nháy viền đỏ và đẩy log vào cột cảnh báo bên phải.

Khi giám thị bấm "Xem Live Stream", nó sẽ subscribe thêm kênh /topic/screenshot/{clientId} để nhận chuỗi Base64 và cập nhật vào thẻ <img id="livestream-img"> liên tục.

4. Hệ thống Thi Trắc Nghiệm (REST API & Frontend)
   Hệ thống thi trắc nghiệm hoạt động độc lập với luồng giám sát TCP, nó sử dụng mô hình RESTful API truyền thống.

Quản lý dữ liệu (QuizApiController.java):

Mọi đề thi và bài làm đều được lưu dưới dạng file .json tĩnh trong thư mục storage/ và storage/results/. Không sử dụng hệ quản trị cơ sở dữ liệu (Database) truyền thống như MySQL, giúp hệ thống cực nhẹ và dễ deploy.

Cung cấp API GET /api/quizzes để lấy danh sách đề thi (Cho cả giám thị và sinh viên).

Cung cấp API POST /api/quizzes để giám thị lưu đề mới.

Cung cấp API POST /api/quizzes/submit để sinh viên nộp bài làm.

Luồng hoạt động của Sinh viên:

Truy cập /login (login.html), nhập MSV, lưu vào sessionStorage bằng file login.js.

Vào phòng chờ / (student_home.html), trang web gọi API /api/quizzes lấy danh sách đề và render các nút "Bắt Đầu Làm Bài".

Vào /take_quiz?id=xxx (take_quiz.html), file take_quiz.js tải dữ liệu câu hỏi từ JSON, hiển thị giao diện thi. Có bộ đếm thời gian. Khi hết giờ hoặc bấm nộp, nó gom mảng đáp án gọi API /submit đẩy lên Server.

Chuyển hướng sang /quiz_result (quiz_result.html) để đối chiếu câu đúng sai tại chỗ.

Tổng kết luồng dữ liệu khi có sự kiện gian lận:
Sinh viên A mở tab "Google Chrome" để tra tài liệu.

Ứng dụng Client (chạy ngầm, không có mã nguồn ở đây) quét thấy chữ "Chrome" không có trong Whitelist.

Client gửi chuỗi: SUSPICIOUS:RED|Chrome qua Socket TCP (Cổng 9999).

TcpServerService (Server) đọc được dòng này, tách chuỗi và gọi notificationService.sendSuspiciousAlertToWeb().

RealtimeNotificationService đóng gói thông tin vào một cục JSON và "bắn" vào kênh WebSocket /topic/alerts.

Trình duyệt của Giám thị (đang mở dashboard.html) nhận được gói JSON qua kênh STOMP.

File app.js lập tức vẽ một thẻ cảnh báo đỏ lòm bên cột phải, đồng thời tìm ô máy tính của Sinh viên A và cho viền nhấp nháy đỏ liên tục!
