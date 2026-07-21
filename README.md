# Nghiên cứu về thuật toán Pseudo-IDList (2020)

## Tổng quan

Repository này được tạo nhằm phục vụ quá trình nghiên cứu, tìm hiểu và tái hiện thuật toán được đề xuất trong bài báo:

> **Efficient algorithms for mining clickstream patterns using pseudo-IDLists**
> *Future Generation Computer Systems, 2020.*

Mục tiêu hiện tại của dự án là **đọc hiểu, chạy thử và tái hiện** cách tác giả hiện thực thuật toán theo đúng nội dung bài báo.

Ở giai đoạn này, **không thực hiện tối ưu, cải tiến hoặc thay đổi thuật toán**.

---

# Mục tiêu nghiên cứu

* Hiểu nguyên lý hoạt động của thuật toán **CUP**.
* Hiểu cấu trúc **Data IDList**.
* Hiểu cấu trúc **pseudo-IDList**.
* Hiểu cơ chế cắt tỉa **DUB (Dynamic Intersection Upper Bound)**.
* Đối chiếu phần hiện thực trong mã nguồn với nội dung bài báo.
* Tái hiện và xác minh kết quả thực nghiệm của bài báo.

---

# Cấu trúc thư mục

```text
original-source/
    Mã nguồn dùng để nghiên cứu và đối chiếu.

datasets/
    Các bộ dữ liệu sử dụng trong bài báo.

```

---

# Tiến độ hiện tại

* [x] Biên dịch thành công dự án.
* [x] Chạy thành công chương trình.
* [x] Xác minh các bộ dữ liệu.
* [x] Phân tích luồng thực thi của chương trình.
* [x] Hiểu cấu trúc Data IDList.
* [x] Hiểu cấu trúc pseudo-IDList.
* [x] Hiểu cơ chế DUB.
* [x] Tái hiện kết quả thực nghiệm của bài báo.

---

# Ghi chú

* Mã nguồn trong repository này **có thể không phải là mã nguồn gốc do tác giả bài báo công bố**.
* Repository được xem như một **bản hiện thực (reproduction/reference implementation)** và sẽ được đối chiếu cẩn thận với nội dung của bài báo.
* Trong giai đoạn hiện tại **không chỉnh sửa, tối ưu hoặc thay đổi thuật toán** cho đến khi đã hiểu đầy đủ toàn bộ quá trình hiện thực.

