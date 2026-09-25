# ChessAndroid (Cờ Tướng)

Ứng dụng Cờ Tướng (Chinese Chess / Xiangqi) cho Android, viết bằng Java, chơi được với AI (negamax + alpha-beta pruning).

Hướng dẫn này dành cho người mới bắt đầu tìm hiểu và đóng góp cho project.

## Yêu cầu môi trường

- **Android Studio** (bản mới, hỗ trợ Gradle 9.x) hoặc IntelliJ IDEA có plugin Android.
- **JDK 17** — project build với `sourceCompatibility`/`targetCompatibility` = 17.
- **Android SDK**: `compileSdk = 35`, `targetSdk = 35`, `minSdk = 24` (Android 7.0+).
- Không cần cài NDK — toàn bộ code là Java, không có phần native (C/C++).

## Clone và mở project

```bash
git clone https://github.com/khoiln218/ChessAndroid.git
cd ChessAndroid
```

Mở thư mục này trực tiếp bằng Android Studio ("Open" → chọn thư mục gốc chứa `settings.gradle`). Android Studio sẽ tự động sync Gradle lần đầu — có thể mất vài phút.

## Build và chạy

- **Debug build** (chạy thử trên máy ảo hoặc điện thoại): dùng nút Run ▶️ trong Android Studio, hoặc:
  ```bash
  ./gradlew assembleDebug
  ```
  Debug build luôn build được, không cần cấu hình gì thêm.

- **Release build** (bản ký để phát hành) cần file `key.properties` ở gốc project (file này bị `.gitignore`, không có trong repo). Xem hướng dẫn tạo keystore trong `key.properties.example`:
  ```bash
  cp key.properties.example key.properties
  # rồi điền storePassword/keyPassword sau khi tạo keystore theo lệnh keytool trong file
  ```
  Nếu không có `key.properties`, project vẫn cấu hình và build debug bình thường — chỉ riêng bản release sẽ không được ký.

## Cấu trúc code

Package gốc: `com.ttnt.chinesechess` (`app/src/main/java/com/ttnt/chinesechess/`)

| Thư mục / file | Vai trò |
|---|---|
| `Menu.java` | Activity khởi động (launcher), màn hình menu chính |
| `Game.java` | Activity màn hình chơi cờ |
| `Settings.java` | Màn hình cài đặt |
| `About.java` | Màn hình thông tin app |
| `ChoiceDialog.java`, `SystemBars.java` | Các thành phần UI phụ trợ |
| `chess/` | Logic cờ tướng thuần (không phụ thuộc Android UI) |
| `chess/Board.java`, `Piece.java`, `State.java` | Bàn cờ, quân cờ, trạng thái ván đấu |
| `chess/C*.java` (`CKing`, `CRook`, `CCannon`, `CKnight`, `CBishop`, `CElephant`, `CPawn`) | Từng loại quân cờ và luật di chuyển riêng |
| `chess/AI.java` | AI đối thủ máy: negamax + alpha-beta pruning, quiescence search, iterative deepening theo thời gian |
| `game/ChineseChessGame.java` | Điều phối một ván cờ (kết nối UI với logic ở `chess/`) |
| `game/TurnTimerView.java` | Đồng hồ đếm giờ lượt đi |
| `graph/` | Vẽ bàn cờ, quân cờ, theme (`Graphics`, `PieceArt`, `BoardTheme`) |

Gợi ý khi mới đọc code: bắt đầu từ `Menu.java` → `Game.java` → `game/ChineseChessGame.java` để hiểu luồng chạy, sau đó vào `chess/Board.java` và `chess/AI.java` để hiểu phần logic/AI.

## Cấu hình chữ ký / thông tin nhạy cảm

Các file sau **không** nằm trong repo (bị `.gitignore`) vì chứa thông tin riêng của từng máy/từng người:
- `local.properties` — đường dẫn SDK cục bộ, Android Studio tự tạo.
- `key.properties` + file `.jks` — thông tin ký app, xem mục Release build ở trên.

Không commit các file này lên git.

## Quy trình đóng góp cơ bản

1. Tạo branch mới từ `master` cho từng thay đổi.
2. Build debug và chạy thử trên máy ảo/điện thoại trước khi commit.
3. Với thay đổi liên quan đến luật cờ hoặc AI, nên tự chơi thử vài ván để kiểm tra hành vi không bị vỡ.
4. Viết commit message ngắn gọn, mô tả đúng thay đổi.
