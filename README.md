# Ghi Chú Minh

Ứng dụng Android ghi chú và nhắc việc hằng ngày cho điện thoại Xiaomi Redmi 10.

## Chức năng

- Thêm công việc / ghi chú mới.
- Nhập nội dung chi tiết cho từng việc.
- Chọn ngày giờ nhắc việc.
- Nhận thông báo nhắc việc trên điện thoại.
- Tùy chọn nhắc lại hằng ngày.
- Bấm vào công việc để đánh dấu hoàn thành hoặc mở lại.
- Nhấn giữ công việc để sửa hoặc xóa.
- Tự đặt lại nhắc việc sau khi khởi động lại điện thoại.
- Lưu dữ liệu cục bộ trên máy, không cần tài khoản đăng nhập.

## Build APK online bằng GitHub Actions

1. Vào tab **Actions** trong repo này.
2. Chọn workflow **Build APK**.
3. Bấm **Run workflow** nếu muốn build thủ công, hoặc chỉ cần push code lên nhánh `main`.
4. Khi workflow chạy xong, mở lần chạy mới nhất.
5. Tải artifact **GhiChuMinh-reminder-debug-apk**.
6. Giải nén artifact và cài file **GhiChuMinh-reminder-debug.apk** trên điện thoại.

## Lưu ý khi cài trên Xiaomi Redmi 10

- Cho phép cài ứng dụng từ nguồn không xác định nếu điện thoại hỏi.
- Khi mở app lần đầu, hãy cho phép quyền thông báo.
- Với máy Xiaomi/MIUI, nên cho app quyền tự khởi động và không hạn chế pin để nhắc việc ổn định hơn.
