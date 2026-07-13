# Ghi Chú Minh v2.0

Ứng dụng Android ghi chú và nhắc việc cá nhân, tối ưu cho công việc văn phòng và nhà máy trên Xiaomi Redmi 10.

## Ba nhóm nâng cấp chính

### 1. Phân loại và ưu tiên
- Nhóm: Nhà máy, Văn phòng, Cá nhân, Khẩn cấp, Ý tưởng.
- Mức ưu tiên: Thấp, Bình thường, Cao, Khẩn cấp.
- Ghim công việc quan trọng lên đầu danh sách.
- Màu thẻ thay đổi theo mức ưu tiên.

### 2. Tìm kiếm và bộ lọc
- Tìm theo tên, nội dung, nhóm và mức ưu tiên.
- Bộ lọc: Tất cả, Hôm nay, Quá hạn, Sắp tới, Đã hoàn thành.
- Hiển thị tổng số việc còn lại và số việc quá hạn.

### 3. Nhắc việc nâng cao
- Nhắc một lần.
- Lặp hằng ngày.
- Lặp từ thứ Hai đến thứ Sáu.
- Lặp hằng tuần.
- Lặp hằng tháng.
- Khôi phục lịch nhắc sau khi khởi động lại điện thoại.

## Chức năng bổ sung
- Giữ và tự chuyển đổi dữ liệu từ bản 1.x.
- Nhận văn bản được chia sẻ từ ứng dụng khác.
- Logo ML hình rồng làm icon và nhận diện trong ứng dụng.
- Mục Đóng góp ý kiến/Báo lỗi gửi về lienhe@mltudonghoa.pro.vn.
- Không yêu cầu đăng nhập, không quảng cáo và lưu dữ liệu cục bộ.

## Build APK bằng GitHub Actions

1. Mở tab **Actions** của repository.
2. Chọn workflow **Build Ghi Chu Minh v2 APK**.
3. Mở lần chạy mới nhất đã thành công.
4. Tải artifact **GhiChuMinh-v2.0-apk**.
5. Giải nén và cài file **GhiChuMinh-v2.0-debug.apk**.

## Thiết lập trên Xiaomi/MIUI

- Cho phép cài ứng dụng từ nguồn không xác định.
- Cho phép quyền thông báo khi mở app lần đầu.
- Bật **Tự khởi động** cho Ghi Chú Minh.
- Đặt chế độ pin thành **Không hạn chế**.
- Cho phép **Báo thức và lời nhắc** nếu điện thoại yêu cầu.

## Phiên bản kỹ thuật

- Application ID: `com.minh.ghichuminh`
- Version code: `3`
- Version name: `2.0.0`
- Min SDK: Android 6.0 (API 23)
- Target SDK: Android 15 (API 35)
