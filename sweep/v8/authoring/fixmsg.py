import glob
p = glob.glob("src/main/java/com/datn/engflow/security/SafeUploadNames.java")[0]
s = open(p, encoding="utf-8-sig").read()
pairs = [
    ('"Thieu ten file tai len"', '"Thiếu tên tệp tải lên"'),
    ('"File tai len phai co duoi moi"', '"Tệp tải lên phải có phần mở rộng"'),
    ('"Duoi file khong hop le"', '"Phần mở rộng tệp không hợp lệ"'),
    ('"Loai file khong duoc ho tro: "', '"Loại tệp không được hỗ trợ: "'),
]
for a, b in pairs:
    n = s.count(a)
    print(("OK " if n == 1 else "MISS ") + a + " count=" + str(n))
    s = s.replace(a, b)
open(p, "w", encoding="utf-8", newline="").write(s)
