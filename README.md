# ChessAndroid (Cờ Tướng)

Ứng dụng Cờ Tướng (Chinese Chess / Xiangqi) cho Android, viết bằng Java, chơi được với AI (minimax, negamax + alpha-beta pruning). Có thể chọn thuật toán cho máy ở màn hình chính: Engine tối ưu, Minimax thuần, Negamax thuần hoặc Alpha-Beta thuần.

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
| `MenuScreen.java` | Activity khởi động (launcher), màn hình menu chính |
| `GameScreen.java` | Activity màn hình chơi cờ |
| `Settings.java` | Màn hình cài đặt |
| `AboutScreen.java` | Màn hình thông tin app |
| `ChoiceDialog.java`, `SystemBars.java` | Các thành phần UI phụ trợ |
| `chess/` | Luật cờ tướng thuần (không phụ thuộc Android UI): bàn cờ, nước đi, luật di chuyển từng quân |
| `chess/Board.java`, `Piece.java`, `Move.java` | Thế cờ (vị trí quân, lượt đi, biên bản ván), quân cờ, nước đi |
| `chess/Rules.java` | Luật chơi: kiểm tra chiếu tướng, sinh nước hợp lệ, chọn/đi quân, thua khi hết nước, xử lặp thế cờ (trường chiếu, trường bắt) |
| `chess/MoveRecord.java` | Một nước đã đi trong biên bản ván cờ: thế cờ sau nước đó, bên đi, có chiếu / đuổi bắt không — dùng để xử luật lặp thế cờ |
| `chess/Point.java` | Một ô trên bàn cờ (x = hàng, y = cột); lớp riêng thay cho `android.graphics.Point`, nên `chess/` là Java thuần, không phụ thuộc Android |
| `chess/PieceCode.java` | Mã quân lưu trong ô bàn cờ (`EMPTY`, `BLACK_KING`…`RED_PAWN`), loại quân (`KING`…`PAWN`) và các hàm `isRed`, `belongsTo`, `kind`, `of` |
| `chess/C*.java` (`CKing`, `CRook`, `CCannon`, `CKnight`, `CBishop`, `CElephant`, `CPawn`) | Từng loại quân cờ và luật di chuyển riêng |
| `ai/` | Thuật toán tìm kiếm thuần, không biết gì về cờ tướng: `GameState` (interface trò chơi), `GameSearch` (thuật toán Negamax dùng chung: `Problem`, `Node`, `search()`), `Negamax` / `AlphaBeta` (chỉ khác nhau ở hàm cắt tỉa truyền vào `Problem`), `Minimax` (dùng lại `Problem`/`Node`, `search()` riêng với hai hàm MAX/MIN) |
| `ai/engine/` | Phần máy chơi: nối luật cờ ở `chess/` với thuật toán ở `ai/` và `ai/optimize/` |
| `ai/engine/ChessState.java` | Áp dụng `GameState` cho cờ tướng (cho Minimax, Negamax, Alpha-Beta): sinh nước, đi/hoàn nước, hết nước = thua, hàm đánh giá |
| `ai/engine/Engine.java` | Máy chơi, dùng chung cho cả 4 thuật toán: `generateMove(bên đi)`; tạo bằng `Engine.minimax(board, depth)`, `negamax(...)`, `alphaBeta(...)`, `optimized(board, depth, budget)` |
| `ai/engine/Evaluation.java` | Hàm đánh giá thế cờ và bảng điểm vị trí của từng quân, dùng chung cho mọi thuật toán |
| `ai/engine/Algorithm.java` | Chọn thuật toán cho máy (Engine tối ưu / Minimax thuần / Negamax thuần / Alpha-Beta thuần) và độ sâu theo cấp độ |
| `ai/optimize/` | Thuật toán tối ưu, cấu trúc theo `ai/`: `OptimizedChessState` ↔ `GameState`, `EnhancedGameSearch` ↔ `GameSearch`, `EnhancedAlphaBeta` ↔ `AlphaBeta` |
| `ai/optimize/EnhancedGameSearch.java` | Tương ứng `GameSearch`: lớp trừu tượng kế thừa `GameSearch.Problem`, cắt tỉa bằng `AlphaBeta.CUTOFF`, chứa thuật toán tối ưu — `search(problem)` gồm iterative deepening theo thời gian, vòng negamax cắt tỉa alpha-beta, bảng chuyển vị, null-move, sắp xếp nước (MVV-LVA, killer, history), quiescence search. `Memory` giữ bảng chuyển vị giữa các nước |
| `ai/optimize/EnhancedAlphaBeta.java` | Tương ứng `AlphaBeta`: kế thừa `EnhancedGameSearch`, là bài toán đưa vào nó — `problem(board, bên đi, độ sâu, budgetMs)`; tự giữ bảng chuyển vị dùng chung (`Memory`) |
| `ai/optimize/OptimizedBoard.java` | Bàn cờ riêng cho `EnhancedAlphaBeta`, kế thừa `Board`: hash Zobrist (cập nhật dần sau mỗi nước), số quân nặng, đường lặp thế cờ, bỏ lượt |
| `ai/optimize/OptimizedChessState.java` | Trạng thái tìm kiếm trên `OptimizedBoard`, hiện thực `GameState`: số nước từ gốc, hàm đánh giá, hết nước = thua, nước giả hợp lệ, nước ăn quân |
| `view/GameView.java` | View điều phối một ván cờ (kết nối UI với logic ở `chess/`) |
| `view/TurnTimerView.java` | Đồng hồ đếm giờ lượt đi |
| `theme/` | Chủ đề: vẽ bàn cờ, quân cờ, bảng màu (`Graphics`, `PieceArt`, `BoardTheme`) |

Gợi ý khi mới đọc code: bắt đầu từ `MenuScreen.java` → `GameScreen.java` → `view/GameView.java` để hiểu luồng chạy, sau đó vào `chess/Board.java` và `ai/` (gồm `ai/engine/`, `ai/optimize/`) để hiểu phần logic/AI.

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
