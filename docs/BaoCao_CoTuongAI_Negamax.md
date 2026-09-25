<!-- cover -->
**HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG**

**KHOA CÔNG NGHỆ THÔNG TIN**

# BÁO CÁO MÔN HỌC: TRÍ TUỆ NHÂN TẠO

**Đề tài: CỜ TƯỚNG AI — ÁP DỤNG THUẬT TOÁN NEGAMAX**

| | |
|---|---|
| Giảng viên hướng dẫn | [Họ tên giảng viên] |
| Sinh viên thực hiện | [Họ tên sinh viên] |
| Mã sinh viên | [Mã sinh viên] |
| Lớp | [Lớp] — Nhóm [..] |
| Học kỳ | Học kỳ 5 |

*[Địa điểm], tháng 9 năm 2026*
<!-- /cover -->

<!-- toc -->

# LỜI MỞ ĐẦU

Trò chơi đối kháng là một trong những bài toán kinh điển và lâu đời nhất của Trí tuệ nhân tạo. Từ chương trình cờ vua đầu tiên của Alan Turing và Claude Shannon vào những năm 1950 cho tới Deep Blue (1997) và AlphaZero (2017), cờ luôn được xem là "con ruồi giấm" của ngành AI: luật chơi rõ ràng, kết quả đo đếm được, nhưng không gian trạng thái lớn đến mức không thể duyệt hết.

Cờ tướng (Xiangqi) là trò chơi trí tuệ phổ biến bậc nhất ở Việt Nam và Trung Quốc. Về mặt lý thuyết, cờ tướng thuộc lớp trò chơi hai người, tổng bằng không, thông tin đầy đủ và tất định — đúng lớp bài toán mà họ thuật toán **Minimax** và biến thể gọn gàng của nó là **Negamax** được xây dựng để giải.

Báo cáo này trình bày quá trình xây dựng ứng dụng **Cờ Tướng AI** trên nền tảng Android, trong đó trọng tâm là "bộ não" của máy: thuật toán **Negamax kết hợp cắt tỉa Alpha-Beta**, cùng các kỹ thuật bổ trợ giải quyết những bài toán phát sinh khi áp dụng Negamax vào một trò chơi thực tế — hiệu ứng đường chân trời, thứ tự nước đi, trạng thái lặp, giới hạn thời gian, luật lặp thế cờ. Mỗi kỹ thuật được trình bày theo trình tự: *bài toán đặt ra → cơ sở lý thuyết → cách cài đặt trong chương trình*. Cuối báo cáo là các số liệu đo đạc thực tế trên chính mã nguồn của đề tài.

Nội dung báo cáo gồm 5 chương:

- **Chương 1 — Tổng quan:** luật cờ tướng và việc mô hình hoá cờ tướng thành một bài toán AI.
- **Chương 2 — Cơ sở lý thuyết:** tìm kiếm đối kháng, Minimax, Negamax, Alpha-Beta và các bài toán liên quan.
- **Chương 3 — Thiết kế và cài đặt:** biểu diễn bàn cờ, sinh nước đi, hàm đánh giá, cài đặt Negamax và các kỹ thuật tối ưu.
- **Chương 4 — Thử nghiệm và đánh giá:** đo số nút duyệt, thời gian, hệ số phân nhánh hiệu dụng.
- **Chương 5 — Kết luận và hướng phát triển.**

# CHƯƠNG 1. TỔNG QUAN ĐỀ TÀI

## 1.1. Giới thiệu trò chơi Cờ Tướng

Cờ tướng được chơi trên bàn cờ gồm **9 cột × 10 hàng giao điểm** (90 vị trí). Giữa bàn cờ là "sông" chia bàn cờ làm hai nửa; mỗi bên có một "cung" kích thước 3×3 giao điểm. Mỗi bên có 16 quân:

| Quân | Số lượng | Cách đi | Ràng buộc đặc biệt |
|---|---|---|---|
| Tướng (帥/將) | 1 | 1 ô theo hàng hoặc cột | Không ra khỏi cung; hai tướng không được đối mặt trên cùng cột mà không có quân cản |
| Sĩ (仕/士) | 2 | 1 ô theo đường chéo | Không ra khỏi cung |
| Tượng (相/象) | 2 | 2 ô theo đường chéo | Không qua sông; bị cản nếu có quân ở "mắt tượng" |
| Mã (馬) | 2 | Hình chữ "nhật" (1 thẳng + 1 chéo) | Bị cản nếu có quân đứng sát ở hướng đi thẳng ("cản chân mã") |
| Xe (車) | 2 | Số ô tuỳ ý theo hàng/cột | Không nhảy qua quân |
| Pháo (炮) | 2 | Như Xe khi di chuyển | Khi ăn quân phải nhảy qua **đúng một** quân làm "ngòi" |
| Tốt (兵/卒) | 5 | 1 ô về phía trước | Sau khi qua sông được đi ngang; không bao giờ đi lùi |

Luật thắng thua quan trọng đối với thiết kế AI:

1. **Chiếu hết** (tướng bị tấn công và không có nước giải) → thua.
2. **Hết nước đi** (không bị chiếu nhưng không còn nước hợp lệ) → cũng tính là **thua**, khác với cờ vua (hoà pat).
3. **Lặp thế cờ:** bên nào liên tục chiếu (trường chiếu) hoặc liên tục đuổi bắt một quân không được bảo vệ (trường bắt) gây ra lặp thế cờ thì bị xử thua; nếu cả hai bên cùng vi phạm hoặc không bên nào vi phạm thì hoà.

## 1.2. Mô hình hoá Cờ Tướng thành bài toán Trí tuệ nhân tạo

Theo cách phân loại môi trường của Russell & Norvig, môi trường cờ tướng có các đặc trưng:

| Đặc trưng | Cờ tướng | Hệ quả đối với thuật toán |
|---|---|---|
| Quan sát được | Đầy đủ (fully observable) | Không cần lưu trạng thái niềm tin; mỗi nút cây là một thế cờ cụ thể |
| Số tác tử | Hai tác tử, đối kháng | Cần tìm kiếm đối kháng (adversarial search) |
| Tính tất định | Tất định (deterministic) | Không có nút "may rủi" (chance node) |
| Tổng lợi ích | Tổng bằng không (zero-sum) | Lợi ích của bên này là thiệt hại của bên kia → nền tảng của **Negamax** |
| Tính tuần tự | Tuần tự, lần lượt | Cây trò chơi xen kẽ hai lớp MAX/MIN |
| Rời rạc / liên tục | Rời rạc | Tập nước đi hữu hạn, liệt kê được |

Tác tử AI trong đề tài là một **tác tử dựa trên lợi ích (utility-based agent)**: tại mỗi lượt, tác tử xây dựng (ngầm) một phần cây trò chơi từ thế cờ hiện tại, ước lượng lợi ích của các thế cờ lá bằng hàm đánh giá, rồi chọn nước đi tối đa hoá lợi ích với giả định đối thủ cũng chơi tối ưu.

## 1.3. Độ phức tạp của bài toán

Hai đại lượng quyết định độ khó của một trò chơi đối với tìm kiếm là **hệ số phân nhánh b** (số nước đi hợp lệ trung bình tại một thế cờ) và **độ sâu d** của ván cờ.

- Đo trực tiếp trên chương trình: thế cờ khai cuộc có **44** nước đi hợp lệ; thế cờ trung cuộc (sau 20 nước, còn 29 quân) có **39** nước. Các nghiên cứu thường lấy b ≈ 38 cho cờ tướng (so với ≈ 35 của cờ vua).
- Một ván cờ tướng trung bình kéo dài khoảng 90–100 nước đơn (ply).
- Độ phức tạp không gian trạng thái ước tính cỡ **10^40**, độ phức tạp cây trò chơi cỡ **10^150** (Allis, 1994) — lớn hơn cờ vua (10^123).

Như vậy việc duyệt toàn bộ cây trò chơi là bất khả thi. Chỉ riêng việc nhìn trước 4 nước đơn với Minimax thuần đã phải duyệt khoảng 44^4 ≈ 3,7 triệu nút (số đo thực tế ở Chương 4: 3.371.871 nút). Đây chính là động lực cho các kỹ thuật cắt tỉa và heuristic trình bày ở Chương 2.

## 1.4. Mục tiêu và phạm vi đề tài

**Mục tiêu:**

- Nghiên cứu cơ sở lý thuyết của tìm kiếm đối kháng: Minimax, Negamax, cắt tỉa Alpha-Beta.
- Phân tích các bài toán phát sinh khi áp dụng Negamax vào cờ tướng thực tế và các kỹ thuật giải quyết.
- Cài đặt một engine cờ tướng dựa trên Negamax trên nền tảng Android, có nhiều cấp độ, trả lời trong thời gian chấp nhận được trên điện thoại.
- Đo đạc, đánh giá hiệu quả của các kỹ thuật tối ưu.

**Phạm vi:** Ứng dụng cho người chơi đấu với máy trên một thiết bị (người cầm quân Đen, máy cầm quân Đỏ). Không bao gồm chơi trực tuyến, khai cuộc/tàn cuộc dựng sẵn (opening book, endgame tablebase) và học máy.

**Công cụ:** Java 17, Android SDK (minSdk 24, targetSdk 35), Android Studio, Gradle. Toàn bộ logic cờ và AI viết bằng Java thuần trong gói `com.ttnt.chinesechess.chess`, không phụ thuộc giao diện.

# CHƯƠNG 2. CƠ SỞ LÝ THUYẾT

## 2.1. Bài toán tìm kiếm đối kháng

Một trò chơi hai người được hình thức hoá thành bài toán tìm kiếm với các thành phần:

| Thành phần | Ý nghĩa | Trong cờ tướng |
|---|---|---|
| S₀ | Trạng thái ban đầu | Thế cờ khai cuộc |
| PLAYER(s) | Bên có lượt đi tại trạng thái s | Đỏ hoặc Đen |
| ACTIONS(s) | Tập nước đi hợp lệ tại s | Các nước không để tướng mình bị chiếu |
| RESULT(s, a) | Mô hình chuyển trạng thái | Thế cờ sau khi thực hiện nước a |
| TERMINAL-TEST(s) | Kiểm tra trạng thái kết thúc | Bị chiếu hết / hết nước / lặp thế cờ |
| UTILITY(s, p) | Hàm lợi ích tại trạng thái kết thúc | +∞ thắng, −∞ thua, 0 hoà |

Tập hợp các trạng thái sinh ra từ S₀ bằng ACTIONS và RESULT tạo thành **cây trò chơi (game tree)**. Mỗi mức của cây tương ứng với một nước đơn (**ply**) của một bên. Người ta quy ước gọi bên cần tối đa hoá điểm là **MAX**, bên còn lại là **MIN**.

## 2.2. Thuật toán Minimax

### 2.2.1. Ý tưởng

Minimax dựa trên giả định **cả hai bên đều chơi tối ưu**. Giá trị minimax của một nút được định nghĩa đệ quy:

```
            ⎧ UTILITY(s)                            nếu s là trạng thái kết thúc
MINIMAX(s) = ⎨ max  { MINIMAX(RESULT(s,a)) | a ∈ ACTIONS(s) }  nếu PLAYER(s) = MAX
            ⎩ min  { MINIMAX(RESULT(s,a)) | a ∈ ACTIONS(s) }  nếu PLAYER(s) = MIN
```

Vì không thể duyệt tới trạng thái kết thúc, trong thực tế ta **giới hạn độ sâu d** và thay UTILITY bằng **hàm đánh giá heuristic** EVAL(s) tại các nút lá (Shannon, 1950).

### 2.2.2. Giả mã

```
function MINIMAX(s, depth, maximizing):
    if depth = 0 or TERMINAL(s):
        return EVAL(s)                      // luôn theo góc nhìn của MAX
    if maximizing:
        best ← −∞
        for each a in ACTIONS(s):
            best ← max(best, MINIMAX(RESULT(s,a), depth − 1, false))
        return best
    else:
        best ← +∞
        for each a in ACTIONS(s):
            best ← min(best, MINIMAX(RESULT(s,a), depth − 1, true))
        return best
```

### 2.2.3. Ví dụ minh hoạ

Xét cây độ sâu 2, hệ số phân nhánh 3. Gốc R là nút MAX, ba con A, B, C là nút MIN, giá trị lá tính theo góc nhìn của MAX:

```
                        R (MAX) = 3
            ┌─────────────┼─────────────┐
         A (MIN)=3     B (MIN)=2     C (MIN)=2
        ┌───┼───┐     ┌───┼───┐     ┌───┼───┐
        3  12   8     2   4   6    14   5   2
```

MIN chọn giá trị nhỏ nhất ở mỗi nhánh: A = 3, B = 2, C = 2. MAX chọn giá trị lớn nhất: R = 3 → nước đi tốt nhất là nước dẫn tới A.

### 2.2.4. Đánh giá

| Tiêu chí | Minimax |
|---|---|
| Tính đầy đủ | Có (nếu cây hữu hạn) |
| Tính tối ưu | Có, khi đối thủ chơi tối ưu |
| Độ phức tạp thời gian | O(b^d) |
| Độ phức tạp bộ nhớ | O(b·d) (duyệt theo chiều sâu) |

Với b ≈ 40 và d = 8: 40^8 ≈ 6,5 × 10^12 nút — không thể thực hiện trên điện thoại. Minimax thuần chỉ khả thi tới độ sâu 3–4.

## 2.3. Thuật toán Negamax

### 2.3.1. Bài toán đặt ra

Giả mã Minimax có hai nhánh gần như đối xứng cho MAX và MIN. Điều này dẫn tới mã nguồn lặp lại, dễ sai khi bổ sung các kỹ thuật tối ưu (mỗi kỹ thuật phải viết hai lần, một cho MAX và một cho MIN). Negamax khai thác tính chất **tổng bằng không** để hợp nhất hai nhánh làm một.

### 2.3.2. Cơ sở toán học

Trong trò chơi tổng bằng không, lợi ích của bên này bằng đúng âm lợi ích của bên kia:

```
UTILITY(s, MAX) = −UTILITY(s, MIN)
```

Kết hợp với đẳng thức cơ bản:

```
min(a, b) = −max(−a, −b)
```

ta thấy phép lấy min của MIN có thể thay bằng phép lấy max trên các giá trị đã đổi dấu. Negamax định nghĩa giá trị của một nút **theo góc nhìn của bên đang có lượt đi** tại nút đó:

```
NEGAMAX(s) = EVAL_side(s)                                        nếu s là lá
NEGAMAX(s) = max { −NEGAMAX(RESULT(s,a)) | a ∈ ACTIONS(s) }      ngược lại
```

trong đó EVAL_side(s) là điểm của thế cờ s **đối với bên sắp đi** tại s.

**Mệnh đề.** Với mọi nút s: NEGAMAX(s) = MINIMAX(s) nếu PLAYER(s) = MAX, và NEGAMAX(s) = −MINIMAX(s) nếu PLAYER(s) = MIN.

**Chứng minh** (quy nạp theo chiều cao của nút):

- *Nút lá:* EVAL_side(s) bằng EVAL(s) nếu MAX đi, bằng −EVAL(s) nếu MIN đi — đúng theo định nghĩa.
- *Nút MAX:* các con c là nút MIN, theo giả thiết quy nạp NEGAMAX(c) = −MINIMAX(c). Do đó NEGAMAX(s) = max(−NEGAMAX(c)) = max(MINIMAX(c)) = MINIMAX(s).
- *Nút MIN:* các con c là nút MAX nên NEGAMAX(c) = MINIMAX(c). Do đó NEGAMAX(s) = max(−MINIMAX(c)) = −min(MINIMAX(c)) = −MINIMAX(s). ∎

Hệ quả: tại gốc (bên AI có lượt đi), Negamax trả về **đúng** giá trị Minimax và chọn **cùng** một nước đi. Negamax không phải là một thuật toán "khác" mà là cách viết gọn của Minimax — độ phức tạp không đổi, O(b^d).

### 2.3.3. Giả mã

```
function NEGAMAX(s, depth):
    if depth = 0 or TERMINAL(s):
        return EVAL_side(s)                 // theo góc nhìn của bên sắp đi
    best ← −∞
    for each a in ACTIONS(s):
        value ← −NEGAMAX(RESULT(s,a), depth − 1)
        best  ← max(best, value)
    return best
```

Một cách viết tương đương thường gặp trong tài liệu dùng hệ số màu color ∈ {+1, −1}: `return color × EVAL(s)` ở lá và gọi đệ quy với `−color`. Trong đề tài, hàm `eval(side)` đã trực tiếp trả về điểm theo góc nhìn của `side`, nên không cần hệ số màu.

### 2.3.4. Ví dụ minh hoạ

Áp dụng Negamax cho cây ở mục 2.2.3. Ở mức lá (sau 2 ply), lượt lại thuộc về MAX nên EVAL_side giữ nguyên giá trị. Tại các nút A, B, C (MIN đi), giá trị con được đổi dấu:

```
NEGAMAX(A) = max(−3, −12, −8) = −3
NEGAMAX(B) = max(−2, −4,  −6) = −2
NEGAMAX(C) = max(−14, −5, −2) = −2
NEGAMAX(R) = max(−(−3), −(−2), −(−2)) = max(3, 2, 2) = 3
```

Kết quả trùng với Minimax: giá trị gốc là 3, nước đi tốt nhất dẫn tới A. Giá trị −3 của A có nghĩa: "với bên đang đi tại A (MIN), thế cờ này là −3", tức là +3 đối với MAX.

### 2.3.5. Ưu điểm và điều kiện áp dụng

**Ưu điểm:**

- Mã nguồn chỉ có **một** hàm đệ quy dùng chung cho cả hai bên, ngắn gọn và dễ bảo trì.
- Mọi kỹ thuật bổ trợ (cắt tỉa, bảng chuyển vị, null-move, tìm kiếm tĩnh...) chỉ cần viết một lần.
- Bảng chuyển vị lưu điểm theo góc nhìn của bên đi — tự nhiên và nhất quán.

**Điều kiện:**

- Trò chơi phải là **tổng bằng không** (hoặc hằng tổng) — cờ tướng thoả mãn.
- Hàm đánh giá phải **đối xứng**: điểm của một thế cờ đối với Đỏ đúng bằng âm điểm của nó đối với Đen. Nếu hàm đánh giá không đối xứng, Negamax sẽ cho kết quả sai.

## 2.4. Cắt tỉa Alpha-Beta dạng Negamax

### 2.4.1. Bài toán đặt ra

Negamax vẫn duyệt toàn bộ b^d nút. Tuy nhiên có những nhánh mà ta **chứng minh được** rằng không thể ảnh hưởng tới quyết định tại gốc; duyệt chúng là lãng phí. Ở ví dụ trên, sau khi biết A = 3, khi duyệt B và gặp lá đầu tiên có giá trị 2, MIN tại B chắc chắn sẽ chọn giá trị ≤ 2 < 3, nên MAX sẽ không bao giờ đi vào B — hai lá còn lại (4 và 6) không cần xét.

### 2.4.2. Ý tưởng

Thuật toán duy trì một **cửa sổ (α, β)**:

- **α** — điểm tốt nhất mà bên đang đi *chắc chắn đạt được* (cận dưới).
- **β** — điểm tối đa mà đối thủ *còn cho phép* bên đang đi đạt được (cận trên); nếu vượt quá β, đối thủ sẽ tránh không đi vào nhánh này từ trước.

Khi một nước đi cho điểm ≥ β, ta **cắt beta (beta cutoff)**: các nước còn lại của nút không cần xét.

Trong dạng Negamax, khi chuyển xuống nút con, góc nhìn đảo ngược nên cửa sổ cũng đảo ngược và đổi dấu: cửa sổ (α, β) của cha trở thành **(−β, −α)** của con. Nhờ vậy, chỉ cần **một** điều kiện cắt `α ≥ β` thay vì hai điều kiện cắt alpha/beta riêng như Minimax.

### 2.4.3. Giả mã

```
function NEGAMAX_AB(s, depth, α, β):
    if depth = 0 or TERMINAL(s):
        return EVAL_side(s)
    best ← −∞
    for each a in ORDER(ACTIONS(s)):          // thứ tự tốt → cắt sớm
        value ← −NEGAMAX_AB(RESULT(s,a), depth − 1, −β, −α)
        if value > best: best ← value
        if best  > α:    α ← best
        if α ≥ β:        break                 // cắt: đối thủ sẽ không cho đi vào đây
    return best

// Lời gọi tại gốc
NEGAMAX_AB(S_current, D, −∞, +∞)
```

Đây là phiên bản **fail-soft**: giá trị trả về có thể nằm ngoài cửa sổ (α, β) và khi đó mang thông tin về cận:

| Giá trị trả về v | Ý nghĩa |
|---|---|
| v ≤ α ban đầu | *Fail-low*: giá trị thật ≤ v (cận trên) |
| α < v < β | Giá trị chính xác |
| v ≥ β | *Fail-high*: giá trị thật ≥ v (cận dưới) |

Ba trường hợp này được dùng trực tiếp làm cờ đánh dấu khi lưu vào bảng chuyển vị (mục 2.6.3).

### 2.4.4. Ví dụ minh hoạ từng bước

Chạy NEGAMAX_AB trên cây ở mục 2.2.3 (giá trị lá tại nút MIN được nhìn theo góc MIN nên đổi dấu):

| Bước | Nút | Cửa sổ (α, β) | Hành động | Kết quả |
|---|---|---|---|---|
| 1 | A | (−∞, +∞) | Xét lá −3 → α = −3; lá −12, −8 không tốt hơn | A trả về −3 |
| 2 | R | (−∞, +∞) | value = 3 → α_R = 3 | |
| 3 | B | (−∞, −3) | Xét lá −2 → α = −2 ≥ β = −3 → **cắt** | B trả về −2; bỏ qua 2 lá (4, 6) |
| 4 | R | (3, +∞) | value = 2 < 3, giữ nguyên | |
| 5 | C | (−∞, −3) | Lá −14 → α = −14; lá −5 → α = −5; lá −2 → α = −2 ≥ −3 → cắt | C trả về −2 |
| 6 | R | (3, +∞) | value = 2 < 3 | Gốc = 3, chọn A |

Kết quả giống hệt Minimax nhưng chỉ duyệt 7/9 lá. Nếu thứ tự các con của C là (2, 14, 5), nhánh C cũng bị cắt ngay sau lá đầu tiên — minh hoạ **thứ tự nước đi quyết định hiệu quả cắt tỉa**.

### 2.4.5. Độ phức tạp

Alpha-Beta **không làm thay đổi kết quả** so với Negamax (tính đúng đắn), chỉ giảm số nút duyệt. Knuth và Moore (1975) chứng minh:

- **Trường hợp xấu nhất** (nước tốt nhất luôn được xét sau cùng): O(b^d) — không cắt được gì.
- **Trường hợp tốt nhất** (nước tốt nhất luôn được xét đầu tiên): số lá phải duyệt là b^⌈d/2⌉ + b^⌊d/2⌋ − 1, tức **O(b^(d/2))**.
- Thứ tự ngẫu nhiên: xấp xỉ O(b^(3d/4)).

Nói cách khác, với thứ tự nước đi tốt, trong cùng thời gian Alpha-Beta có thể **nhìn sâu gấp đôi** Minimax. Với b = 40, d = 8: Minimax cần 40^8 ≈ 6,5 × 10^12 lá, còn Alpha-Beta tối ưu chỉ cần 2 × 40^4 − 1 ≈ 5,1 × 10^6 lá.

## 2.5. Hàm đánh giá tĩnh

Vì không duyệt được tới cuối ván, Negamax cần một **hàm đánh giá tĩnh (static evaluation function)** EVAL(s) để ước lượng mức độ thuận lợi của thế cờ lá. Yêu cầu đối với hàm đánh giá:

1. **Tương quan** với xác suất thắng thực sự.
2. **Nhanh**: hàm được gọi tại hầu hết các nút lá — là đoạn mã chạy nhiều nhất của cả chương trình.
3. **Đối xứng** giữa hai bên (điều kiện của Negamax, mục 2.3.5).

Dạng phổ biến là tổ hợp tuyến tính các đặc trưng:

```
EVAL(s) = w₁·f₁(s) + w₂·f₂(s) + ... + wₙ·fₙ(s)
```

trong đó các đặc trưng thường dùng cho cờ tướng là: **giá trị vật chất** (Xe > Pháo ≈ Mã > Tượng ≈ Sĩ > Tốt), **vị trí quân** (bảng điểm theo ô — piece-square table), **an toàn tướng**, **mức độ phát triển quân**. Đề tài sử dụng bảng điểm theo ô gộp cả giá trị vật chất và vị trí, cộng thêm hai thành phần bổ trợ (chi tiết ở mục 3.4).

## 2.6. Các bài toán liên quan khi áp dụng Negamax và kỹ thuật giải quyết

Negamax + Alpha-Beta thuần tuý vẫn còn nhiều hạn chế khi áp dụng vào một trò chơi thực tế. Mục này trình bày từng bài toán cùng kỹ thuật giải quyết được sử dụng trong đề tài.

### 2.6.1. Hiệu ứng đường chân trời → Tìm kiếm tĩnh (Quiescence Search)

**Bài toán.** Khi dừng tìm kiếm tại độ sâu cố định, thuật toán đánh giá thế cờ ngay cả khi đang giữa một cuộc đổi quân. Ví dụ: ở ply cuối, Xe ăn một Tốt được bảo vệ — hàm đánh giá thấy "được thêm một Tốt", nhưng nước tiếp theo (nằm ngoài "đường chân trời" tìm kiếm) đối phương ăn lại Xe. AI sẽ đánh giá sai hoàn toàn và chủ động đi những nước thí quân vô lý. Đây gọi là **hiệu ứng đường chân trời (horizon effect)**.

**Giải pháp.** Tại nút lá, thay vì đánh giá ngay, ta chạy tiếp một tìm kiếm Negamax thu hẹp chỉ gồm **các nước ăn quân** cho tới khi thế cờ "yên tĩnh" (không còn nước ăn quân đáng kể):

```
function QUIESCE(s, α, β):
    stand ← EVAL_side(s)                 // "đứng yên": bên đi không bắt buộc phải ăn
    if stand ≥ β: return stand
    α ← max(α, stand)
    for each capture c in ORDER_MVV_LVA(CAPTURES(s)):
        if stand + value(victim(c)) + DELTA < α: break     // cắt delta
        value ← −QUIESCE(RESULT(s,c), −β, −α)
        if value ≥ β: return value
        α ← max(α, value)
    return α
```

Giá trị **stand-pat** là cận dưới vì bên đi luôn có quyền không ăn quân. **Cắt delta (delta pruning)**: nếu ngay cả khi ăn được quân đó cộng thêm một biên an toàn vẫn không đạt α thì bỏ qua nước ăn này và các nước sau (vì đã sắp theo giá trị quân bị ăn giảm dần).

### 2.6.2. Thứ tự nước đi → MVV-LVA, nước sát thủ, heuristic lịch sử, nước từ bảng băm

**Bài toán.** Như phân tích ở mục 2.4.5, hiệu quả của Alpha-Beta phụ thuộc hoàn toàn vào việc nước tốt nhất có được xét sớm hay không. Nếu các nước được xét theo thứ tự sinh ra (quét bàn cờ từ trên xuống), Alpha-Beta gần như suy biến về Minimax.

**Giải pháp.** Gán điểm ưu tiên cho từng nước và xét theo thứ tự giảm dần:

| Ưu tiên | Loại nước | Lý do |
|---|---|---|
| 1 | **Nước từ bảng chuyển vị (hash move)** | Nước tốt nhất đã tìm được ở lần duyệt trước của cùng thế cờ |
| 2 | **Nước ăn quân theo MVV-LVA** (*Most Valuable Victim – Least Valuable Attacker*) | Ăn quân giá trị cao bằng quân giá trị thấp trước: điểm = 10 × giá trị quân bị ăn − giá trị quân ăn |
| 3 | **Nước sát thủ (killer moves)** | Nước yên tĩnh (không ăn quân) từng gây cắt beta tại **cùng độ sâu** ở nhánh anh em — rất có thể cũng bác bỏ được nhánh hiện tại |
| 4 | **Heuristic lịch sử (history heuristic)** | Bảng đếm [ô đi][ô đến] cộng depth² mỗi khi nước đó gây cắt tỉa ở bất kỳ đâu trong cây |

### 2.6.3. Trạng thái lặp lại (transposition) → Bảng chuyển vị với khoá Zobrist

**Bài toán.** Cùng một thế cờ có thể đạt được bằng nhiều thứ tự nước đi khác nhau (ví dụ: Xe đi trước rồi Mã, hoặc Mã đi trước rồi Xe). Cây trò chơi thực chất là một **đồ thị**, và Negamax thuần sẽ duyệt lại cùng một cây con nhiều lần.

**Giải pháp.** Dùng **bảng chuyển vị (transposition table — TT)**, một bảng băm lưu kết quả đã tính cho từng thế cờ: *khoá, độ sâu đã tìm, điểm, loại cận (EXACT/LOWER/UPPER), nước tốt nhất*. Trước khi duyệt một nút, tra bảng:

- Nếu mục lưu có độ sâu ≥ độ sâu cần tìm và loại cận cho phép (EXACT; hoặc LOWER với điểm ≥ β; hoặc UPPER với điểm ≤ α) → trả về luôn, không duyệt.
- Nếu không dùng được điểm, vẫn lấy **nước tốt nhất** đã lưu để xét đầu tiên (hash move).

Khoá của thế cờ được tính bằng **băm Zobrist** (Zobrist, 1970): sinh trước một số ngẫu nhiên 64-bit cho mỗi cặp (ô, quân) và một số cho lượt đi. Khoá của thế cờ là phép XOR của các số ứng với mọi quân đang có trên bàn. Vì XOR là phép tự nghịch đảo (x ⊕ k ⊕ k = x), việc cập nhật khoá khi đi hoặc lùi một nước chỉ cần 3–4 phép XOR thay vì quét lại 90 ô:

```
key ← key ⊕ Z[from][piece] ⊕ Z[to][piece] ⊕ Z[to][captured] ⊕ Z_side
```

### 2.6.4. Giới hạn thời gian → Tìm kiếm sâu dần (Iterative Deepening)

**Bài toán.** Thời gian tìm kiếm tăng theo hàm mũ với độ sâu và thay đổi mạnh theo từng thế cờ. Nếu cố định độ sâu, có thế cờ máy trả lời tức thì, có thế cờ máy "treo" hàng phút. Nếu đặt giới hạn thời gian cho một lần tìm cố định độ sâu, khi hết giờ giữa chừng ta không có kết quả nào đáng tin.

**Giải pháp.** Lần lượt chạy Negamax với độ sâu 1, 2, 3, ... cho tới khi hết thời gian hoặc đạt độ sâu tối đa; luôn giữ lại kết quả của **lần lặp gần nhất đã hoàn thành**. Chi phí lặp lại các độ sâu nông là nhỏ (tổng chi phí các lần lặp trước chỉ bằng khoảng 1/(b−1) lần lặp cuối), và còn được bù lại: nước tốt nhất của lần lặp trước được xét đầu tiên ở lần lặp sau, và bảng chuyển vị/killer/history đã "học" được từ các lần trước — giúp cắt tỉa nhiều hơn hẳn.

### 2.6.5. Tìm kiếm cửa sổ hẹp → Principal Variation Search (PVS)

**Bài toán.** Khi thứ tự nước đi đã tốt, nước đầu tiên thường là nước tốt nhất. Các nước còn lại chỉ cần *chứng minh là kém hơn*, không cần biết chính xác kém bao nhiêu.

**Giải pháp.** Nước đầu tiên được duyệt với cửa sổ đầy đủ (−β, −α). Các nước sau được duyệt với **cửa sổ rỗng (null window)** (−α−1, −α) — tìm kiếm với cửa sổ rỗng cắt tỉa được nhiều hơn rất nhiều. Chỉ khi kết quả rơi vào khoảng α < v < β (nước này hoá ra tốt hơn), mới duyệt lại với cửa sổ đầy đủ. Kỹ thuật này còn gọi là **NegaScout** (Reinefeld, 1983) — một cải tiến trực tiếp trên nền Negamax.

### 2.6.6. Cắt tỉa tiến → Null-move Pruning và bài toán Zugzwang

**Bài toán.** Alpha-Beta là cắt tỉa *lùi* (backward pruning) — an toàn tuyệt đối nhưng giới hạn ở O(b^(d/2)). Muốn nhìn sâu hơn cần mạnh dạn bỏ qua các nhánh "gần như chắc chắn" vô ích.

**Giải pháp.** Giả sử bên đi **bỏ lượt** (null move) và tìm kiếm với độ sâu giảm R (R = 2 hoặc 3). Nếu ngay cả khi nhường lượt cho đối thủ mà điểm vẫn ≥ β, thì với một nước đi thực sự điểm càng cao hơn → cắt ngay cả cây con. Lập luận này dựa trên giả định "được đi luôn tốt hơn bỏ lượt".

Giả định đó **sai** trong các thế **zugzwang** — mọi nước đi đều làm thế cờ xấu đi. Vì vậy null-move chỉ được dùng khi: bên đi không bị chiếu (bị chiếu thì không thể bỏ lượt), bên đi còn quân mạnh (Xe/Mã/Pháo — thế chỉ còn Tướng, Sĩ, Tượng, Tốt dễ rơi vào zugzwang), và nút đủ sâu để khoản tiết kiệm lớn hơn chi phí thử.

### 2.6.7. Chọn đường chiếu hết ngắn nhất → Điểm chiếu hết theo khoảng cách

**Bài toán.** Nếu mọi thế bị chiếu hết đều có cùng điểm −∞, AI không phân biệt được "chiếu hết sau 2 nước" và "chiếu hết sau 6 nước"; nó có thể lòng vòng mãi không kết thúc ván, hoặc khi thua thì không cố kéo dài.

**Giải pháp.** Điểm của thế bị chiếu hết là **−(MATE − ply)** với ply là khoảng cách tới gốc. Chiếu hết càng sớm, điểm (đối với bên thắng) càng cao. Khi lưu vào bảng chuyển vị, điểm chiếu hết được quy đổi về khoảng cách tính từ chính thế cờ đó (không phải từ gốc), và quy đổi ngược lại khi đọc ra.

### 2.6.8. Lặp thế cờ, chiếu mãi → Phát hiện lặp trong cây tìm kiếm

**Bài toán.** Negamax giả định cây là hữu hạn và không có chu trình. Trong thực tế, một bên có thể chiếu qua chiếu lại vô hạn; nếu không xử lý, AI có thể đánh giá một chuỗi chiếu vô tận là "thắng", hoặc đi tới đi lui một quân vì không tìm ra nước nào tốt hơn.

**Giải pháp.** Trong quá trình tìm kiếm, lưu khoá Zobrist của các thế cờ trên đường đi hiện tại và của toàn bộ lịch sử ván đấu. Nếu thế cờ hiện tại trùng với một thế đã xuất hiện, gán điểm **0 (hoà)** cho nút đó. Ở cấp độ ván cờ, bộ xử lý luật áp dụng luật trường chiếu / trường bắt để xử thua bên vi phạm.

# CHƯƠNG 3. THIẾT KẾ VÀ CÀI ĐẶT

## 3.1. Kiến trúc tổng thể

Ứng dụng tách bạch phần logic cờ/AI (Java thuần) với phần giao diện Android:

| Thành phần | Vai trò |
|---|---|
| `Menu`, `Game`, `Settings`, `About` | Các Activity: menu chính, màn hình chơi, cài đặt, thông tin |
| `game/ChineseChessGame` | Điều phối ván cờ: nhận chạm của người chơi, gọi AI trên luồng nền, xử lý kết thúc ván |
| `game/TurnTimerView` | Đồng hồ đếm giờ cho mỗi lượt |
| `graph/` | Vẽ bàn cờ, quân cờ, giao diện (theme) |
| `chess/Board` | Biểu diễn bàn cờ, sinh nước đi, kiểm tra chiếu, khoá Zobrist, luật lặp thế cờ |
| `chess/Piece` và `CKing`, `CBishop` (Sĩ), `CElephant` (Tượng), `CKnight`, `CRook`, `CCannon`, `CPawn` | Luật di chuyển và bảng điểm vị trí của từng loại quân |
| `chess/State` | Một nước đi: ô đi, ô đến, quân đi, quân bị ăn |
| `chess/AI` | **Engine Negamax**: Alpha-Beta, PVS, bảng chuyển vị, null-move, tìm kiếm tĩnh, tìm kiếm sâu dần |

Luồng một lượt của máy: `ChineseChessGame.computer()` → chạy `AI.generateMove(side)` trên một luồng nền (`ExecutorService` đơn luồng) để không làm đơ giao diện → kết quả được gửi về luồng giao diện qua `Handler` → thực hiện nước đi và kiểm tra kết thúc ván.

## 3.2. Biểu diễn bàn cờ và nước đi

Bàn cờ là mảng hai chiều `byte[10][9]`. Mỗi ô chứa mã quân:

| Quân | Tướng | Sĩ | Tượng | Mã | Xe | Pháo | Tốt |
|---|---|---|---|---|---|---|---|
| Đen (người chơi) | 8 | 9 | 10 | 11 | 12 | 13 | 14 |
| Đỏ (máy) | 15 | 16 | 17 | 18 | 19 | 20 | 21 |

Giá trị 0 là ô trống. Cách mã hoá này cho phép xác định màu quân bằng một phép so sánh (`value > 14` là quân Đỏ) và loại quân bằng một phép trừ.

Một nước đi (`State`) lưu **ô đi, ô đến, quân đi và quân bị ăn**. Nhờ lưu quân bị ăn, một nước đi có thể được hoàn tác chỉ từ chính nó (`doMove` / `reMove`) mà không cần sao chép bàn cờ — điều bắt buộc khi thuật toán thực hiện hàng triệu lần đi/lùi mỗi lượt:

```java
void doMove(State s) {
    clone.cell[s.to.x][s.to.y] = s.piece;
    clone.cell[s.from.x][s.from.y] = 0;
    toggle(s);                        // cập nhật khoá Zobrist bằng XOR
}

void reMove(State s) {
    clone.cell[s.to.x][s.to.y] = s.captured;
    clone.cell[s.from.x][s.from.y] = s.piece;
    toggle(s);                        // XOR lần nữa = hoàn tác
}
```

## 3.3. Sinh nước đi và kiểm tra chiếu tướng

Mỗi lớp quân cài đặt phương thức `generate()` theo luật riêng (Mã kiểm tra cản chân, Pháo tìm ngòi, Tốt kiểm tra đã qua sông...). Mọi nước đi đều đi qua một điểm chung `Piece.offer()`, nơi có hai tuỳ chọn phục vụ tìm kiếm:

- `capturesOnly` — chỉ sinh nước ăn quân (dùng cho tìm kiếm tĩnh).
- `legalOnly` — chỉ giữ nước không để tướng mình bị chiếu.

**Sinh nước giả hợp lệ (pseudo-legal).** Kiểm tra "tướng có bị chiếu không" là thao tác tốn kém nhất. Trong Negamax, engine sinh nước *giả hợp lệ* (bỏ qua kiểm tra này) và chỉ kiểm tra khi thực sự đi nước đó. Vì Alpha-Beta thường cắt sau 1–2 nước đầu, phần lớn các nước còn lại không bao giờ phải kiểm tra.

**Kiểm tra chiếu nhìn từ tướng (`Board.kingSafe`).** Thay vì hỏi từng quân địch "có tấn công được tướng không", hàm nhìn ra từ vị trí tướng: 4 tia thẳng phát hiện Xe, Pháo (nhảy qua đúng một ngòi) và luật hai tướng đối mặt; 8 vị trí Mã (có kiểm tra cản chân); 3 vị trí Tốt. Sĩ và Tượng bị bỏ qua vì không bao giờ rời nửa bàn cờ của mình nên không thể tấn công tướng địch.

## 3.4. Hàm đánh giá

Hàm `eval(side)` trả về điểm theo góc nhìn của `side` — đúng yêu cầu của Negamax. Điểm gồm ba thành phần.

**(1) Bảng điểm theo ô (piece-square table).** Mỗi loại quân có một bảng 10×9 cho biết giá trị của quân đó tại từng ô. Giá trị trong bảng đã bao gồm cả giá trị vật chất (Xe ≈ 90, Pháo ≈ 50, Mã ≈ 40, Tượng ≈ 22–28, Sĩ ≈ 19–22, Tốt 0–23) lẫn giá trị vị trí. Ví dụ bảng của quân Xe (nhìn từ phía Đen):

```
{90, 90, 90, 90, 90, 90, 90, 90, 90},
{90, 92, 91, 91, 90, 91, 91, 92, 90},
{90, 91, 90, 90, 90, 90, 90, 91, 90},
{90, 91, 90, 91, 90, 91, 90, 91, 90},
{90, 93, 90, 91, 90, 91, 90, 93, 90},
{90, 94, 90, 94, 90, 94, 90, 94, 90},   // Xe ở bờ sông được cộng điểm
{90, 91, 90, 91, 90, 91, 90, 91, 90},
{90, 92, 90, 91, 90, 91, 90, 92, 90},
{91, 92, 90, 93, 90, 93, 90, 92, 91},
{89, 92, 90, 90, 90, 90, 90, 92, 89}    // Xe nằm ở góc bị trừ điểm
```

Quân Tốt chưa qua sông gần như không có giá trị (0–15), qua sông tăng lên 20–23 và cao nhất khi áp sát cung tướng. Bảng của Đỏ được suy ra bằng cách **lật ngược và đối xứng** bảng của Đen, đảm bảo tính đối xứng của hàm đánh giá. Để tăng tốc, tất cả các bảng được "làm phẳng" một lần khi nạp lớp thành mảng `PIECE_SQUARE[mã quân][ô]` đã mang sẵn dấu (+ cho Đỏ, − cho Đen), nên đánh giá một thế cờ chỉ còn là một vòng lặp cộng qua 90 ô.

**(2) Thưởng tấn công khi đối phương thiếu quân phòng thủ (`attackBonus`).** Bên thiếu Sĩ thì dễ bị Pháo và Mã tấn công; thiếu Tượng thì dễ bị Pháo và Xe tấn công:

```
nếu đối phương còn < 2 Sĩ:    + 2 × (số Pháo) + (số Mã)
nếu đối phương còn < 2 Tượng: + 2 × (số Pháo) + (số Xe)
```

**(3) Phạt Xe chưa phát triển (`development`).** Xe còn nằm ở góc xuất phát và bị Mã của mình chắn: −5 điểm mỗi Xe.

Công thức tổng: `eval(side) = PST(side) + attackBonus(ta) − attackBonus(địch) + development(ta) − development(địch)`, trong đó PST(side) là tổng bảng điểm đã đổi dấu theo góc nhìn của `side`. Toàn bộ được tính trong **một lần quét** bàn cờ.

## 3.5. Cài đặt Negamax với Alpha-Beta

Phương thức trung tâm `AI.alphaBeta(depth, side, alpha, beta, ply)` là cài đặt trực tiếp của giả mã NEGAMAX_AB ở mục 2.4.3, tích hợp các kỹ thuật ở mục 2.6. Dưới đây là mã nguồn trích lược kèm chú thích:

```java
public int alphaBeta(int depth, boolean side, int alpha, int beta, int ply) {
    nodes++;
    if (outOfTime()) return 0;                          // hết giờ: kết quả sẽ bị bỏ
    if (ply > 0 && repeats(ply)) return 0;              // lặp thế cờ → hoà   (2.6.8)
    if (depth <= 0) return quiesce(side, alpha, beta, ply);   // lá → tìm kiếm tĩnh (2.6.1)

    // (2.6.3) Tra bảng chuyển vị
    int slot = (int) (hash & (TT_SIZE - 1));
    int wanted = 0;
    if (ttKey[slot] == hash) {
        wanted = ttMove[slot];                          // hash move để xét trước
        if (ttDepth[slot] >= depth) {
            int stored = fromTT(ttScore[slot], ply);
            byte flag = ttFlag[slot];
            if (flag == TT_EXACT
                    || (flag == TT_LOWER && stored >= beta)
                    || (flag == TT_UPPER && stored <= alpha)) return stored;
        }
    }

    // (2.6.6) Null-move: không bị chiếu, còn Xe/Mã/Pháo, đủ sâu, cửa sổ rỗng
    if (depth >= 3 && beta - alpha == 1 && (side ? heavyRed : heavyBlack) > 0
            && clone.kingSafe(side)) {
        int reduction = depth > 6 ? 3 : 2;
        hash ^= Board.ZOBRIST_SIDE;                     // "bỏ lượt": chỉ đổi bên đi
        int value = -alphaBeta(depth - 1 - reduction, !side, -beta, -beta + 1, ply + 1);
        hash ^= Board.ZOBRIST_SIDE;
        if (value >= beta && !aborted) return value >= MATE_BOUND ? beta : value;
    }

    // Sinh nước giả hợp lệ và chấm điểm thứ tự (2.6.2)
    ArrayList<State> moves = listAt(ply);
    clone.collect(side, false, false, moves);
    int[] score = scoreMoves(moves, wanted, ply);

    int alphaOrig = alpha, best = -INF;
    State bestHere = null;
    boolean first = true, any = false;
    for (int i = 0; i < moves.size(); i++) {
        promote(moves, score, i);                       // đưa nước tốt nhất còn lại lên vị trí i
        State m = moves.get(i);
        doMove(m);
        if (!clone.kingSafe(side)) { reMove(m); continue; }   // nước không hợp lệ
        any = true;
        int value;
        if (first) {                                    // (2.6.5) PVS: nước đầu, cửa sổ đầy đủ
            value = -alphaBeta(depth - 1, !side, -beta, -alpha, ply + 1);
        } else {                                        // các nước sau: cửa sổ rỗng
            value = -alphaBeta(depth - 1, !side, -alpha - 1, -alpha, ply + 1);
            if (value > alpha && value < beta)          // bất ngờ tốt hơn → duyệt lại
                value = -alphaBeta(depth - 1, !side, -beta, -alpha, ply + 1);
        }
        reMove(m);
        first = false;

        if (value > best) { best = value; bestHere = m; }   // ← lõi Negamax
        if (best > alpha) alpha = best;
        if (alpha >= beta) { remember(m, depth, ply); break; }   // cắt beta + ghi killer/history
    }
    if (!any) return -(MATE - ply);                     // hết nước = thua (2.6.7)

    // Lưu bảng chuyển vị với loại cận tương ứng (fail-soft, mục 2.4.3)
    if (!aborted) {
        byte flag = best <= alphaOrig ? TT_UPPER : (best >= beta ? TT_LOWER : TT_EXACT);
        if (ttKey[slot] != hash || ttDepth[slot] <= depth) {
            ttKey[slot] = hash;  ttScore[slot] = toTT(best, ply);
            ttDepth[slot] = (byte) depth;  ttFlag[slot] = flag;
            ttMove[slot] = bestHere == null ? 0 : code(bestHere);
        }
    }
    return best;
}
```

Một số điểm cài đặt đáng chú ý:

- **Lõi Negamax** thể hiện ở biểu thức `-alphaBeta(depth - 1, !side, -beta, -alpha, ply + 1)`: đổi bên đi (`!side`), đảo và đổi dấu cửa sổ, đổi dấu giá trị trả về. Không có nhánh riêng cho MAX/MIN.
- **Hằng số điểm:** `INF = 1.000.000` (lớn hơn mọi điểm có thể), `MATE = 900.000`. Điểm có trị tuyệt đối ≥ `MATE_BOUND = MATE − 1000` được hiểu là điểm chiếu hết và được quy đổi khi ghi/đọc bảng chuyển vị (`toTT` / `fromTT`).
- **Bảng chuyển vị** có 2^17 = 131.072 mục, chỉ số là `hash & (TT_SIZE − 1)`. Lưu dưới dạng 5 mảng song song (khoá, điểm, nước, độ sâu, cờ) để tránh tạo đối tượng. Chính sách thay thế: ghi đè nếu khác thế cờ hoặc độ sâu mới ≥ độ sâu cũ.
- **Chấm điểm thứ tự nước đi** (`moveScore`): hash move 2^26; nước ăn quân 2^22 + 16 × giá trị quân bị ăn − giá trị quân ăn; killer thứ nhất 2^21 + 1, killer thứ hai 2^21; còn lại theo bảng history. Giá trị quân dùng để sắp xếp: Xe 900, Pháo 450, Mã 400, Tượng 220, Sĩ 200, Tốt 100.
- **Sắp xếp lười (`promote`)**: thay vì sắp xếp toàn bộ danh sách trước, mỗi bước chỉ tìm nước có điểm cao nhất trong phần chưa duyệt (sắp xếp chọn từng bước). Khi nút cắt ngay sau 1–2 nước, phần sắp xếp còn lại được tiết kiệm.
- **Cập nhật killer/history** (`remember`): chỉ với nước yên tĩnh gây cắt; history cộng thêm depth² (cắt ở nút sâu có trọng số lớn hơn).
- **Tái sử dụng bộ nhớ:** mỗi mức ply có sẵn một danh sách nước đi và một mảng điểm dùng lại (`listAt`, `scores`), vì tại một thời điểm chỉ có một nút ở mỗi mức đang được duyệt. Điều này loại bỏ phần lớn rác bộ nhớ — quan trọng trên Android.

## 3.6. Cài đặt tìm kiếm tĩnh

```java
private int quiesce(boolean side, int alpha, int beta, int ply) {
    nodes++;
    if (outOfTime()) return 0;
    int stand = eval(side);                              // stand-pat
    if (stand >= beta) return stand;
    if (stand > alpha) alpha = stand;
    if (ply >= maxDepth + QUIET_PLIES) return stand;     // giới hạn thêm tối đa 4 ply

    if (!clone.hasLegalMove(side)) return -(MATE - ply); // bị chiếu hết / hết nước
    ArrayList<State> moves = listAt(ply);
    clone.collect(side, true, true, moves);              // chỉ nước ăn quân, hợp lệ
    moves.sort(BY_CAPTURE);                              // MVV-LVA

    int best = stand;
    for (State m : moves) {
        if (stand + evalValue(m.captured) + DELTA < alpha) break;   // cắt delta (DELTA = 25)
        doMove(m);
        int value = -quiesce(!side, -beta, -alpha, ply + 1);        // vẫn là Negamax
        reMove(m);
        if (value > best) best = value;
        if (best > alpha) alpha = best;
        if (alpha >= beta) break;
    }
    return best;
}
```

Tìm kiếm tĩnh cũng là một Negamax với cửa sổ Alpha-Beta, chỉ khác ở tập nước đi (chỉ ăn quân) và việc dùng stand-pat làm cận dưới. Độ sâu mở rộng bị giới hạn bởi `QUIET_PLIES = 4` để tránh bùng nổ trong những thế có chuỗi ăn quân dài.

## 3.7. Tìm kiếm sâu dần, quản lý thời gian và cấp độ

```java
public State generateMove(boolean side) {
    clone = new Board(board);                          // làm việc trên bản sao
    deadline = System.currentTimeMillis() + budgetMs;
    hash = zobristOf(side);
    ... // khởi tạo lịch sử ván đấu, xoá killer/history
    ArrayList<State> rootMoves = new ArrayList<>();
    clone.collect(side, false, true, rootMoves);       // nước hợp lệ tại gốc
    rootMoves.sort(BY_CAPTURE);
    State bestMove = rootMoves.get(0);                 // luôn có nước để đi

    for (int depth = 1; depth <= maxDepth; depth++) {
        if (depth > 1 && !worthDeepening(started)) break;
        aborted = false;
        State found = searchRoot(rootMoves, depth, side);
        if (aborted) break;                            // lần lặp dở dang → bỏ
        bestMove = found;
        rootMoves.remove(found);
        rootMoves.add(0, found);                       // nước tốt nhất xét đầu ở lần sau
    }
    return bestMove;
}
```

- **Kiểm tra thời gian** (`outOfTime`) được thực hiện 256 nút một lần để giảm chi phí gọi đồng hồ hệ thống. Khi hết giờ, cờ `aborted` được bật và toàn bộ lần lặp dở dang bị loại bỏ.
- **Quyết định có đào sâu thêm không** (`worthDeepening`): một ply mới tốn khoảng 5–6 lần tổng thời gian đã dùng, nên chỉ bắt đầu ply mới khi thời gian đã dùng **< 1/8 ngân sách**. Bắt đầu một ply rồi bỏ dở là lãng phí toàn bộ thời gian còn lại.

**Cấp độ chơi.** Người dùng chọn một trong ba cấp độ; mỗi cấp độ tăng độ sâu thêm **2 ply** (một cặp nước của hai bên — đủ để "nhìn thấy" một cuộc trao đổi quân trọn vẹn) và nhân ngân sách thời gian lên 4 lần (xấp xỉ chi phí của một ply):

| Cấp độ | Tham số `level` | Độ sâu tối đa (ply) | Ngân sách thời gian |
|---|---|---|---|
| Dễ | 2 | 4 | 1,5 giây |
| Khó | 3 | 6 | 6 giây |
| Cực khó | 4 | 8 | 24 giây |

Ngân sách là **trần** chứ không phải mục tiêu: trong điều kiện bình thường engine đạt độ sâu tối đa sớm hơn nhiều (xem Chương 4); ngân sách chỉ phát huy tác dụng trên thiết bị chậm hoặc thế cờ phức tạp.

## 3.8. Xử lý luật lặp thế cờ

Trong tìm kiếm, `repeats(ply)` so khoá hiện tại với các khoá cùng lượt đi trên đường đi (bước nhảy 2 ply) và với toàn bộ khoá của lịch sử ván đấu; trùng thì trả 0 (hoà). Điều này ngăn AI coi một chuỗi chiếu vô tận là thắng lợi.

Ở cấp độ ván đấu, `Board.judgeRepetition()` phát hiện một thế cờ lặp lại lần thứ ba, rồi xét chu trình giữa hai lần xuất hiện: bên nào **mọi nước** trong chu trình đều chiếu (trường chiếu) hoặc đều đuổi bắt một quân không được bảo vệ (trường bắt) thì bị xử thua; nếu cả hai hoặc không bên nào như vậy thì xử hoà.

# CHƯƠNG 4. THỬ NGHIỆM VÀ ĐÁNH GIÁ

## 4.1. Môi trường thử nghiệm

Để đo chính xác, gói `chess` (không phụ thuộc giao diện) được biên dịch và chạy trên máy tính với JDK 21 (hai lớp Android `Point` và `Log` được thay bằng lớp giả tối giản). Mã nguồn thuật toán được giữ **nguyên vẹn**. Mỗi phép đo chạy 3 lần, lấy thời gian nhỏ nhất. Hai thế cờ thử nghiệm:

- **Khai cuộc:** thế cờ ban đầu, Đỏ đi, 44 nước hợp lệ — thế cờ "rộng" nhất của ván.
- **Trung cuộc:** thế cờ sau 20 nước đơn do engine tự chơi ở cấp Dễ, còn 29 quân, 39 nước hợp lệ.

*Lưu ý:* thời gian trên điện thoại thực tế chậm hơn máy tính vài lần; số nút duyệt thì không phụ thuộc thiết bị.

## 4.2. So sánh Negamax thuần và Negamax + Alpha-Beta

Thí nghiệm này dùng ba phiên bản tối giản (cùng hàm đánh giá, cùng bộ sinh nước, **không** có tìm kiếm tĩnh và các kỹ thuật khác) để cô lập hiệu quả của cắt tỉa và thứ tự nước đi:

**Thế cờ khai cuộc:**

| Độ sâu | Negamax thuần (nút) | Alpha-Beta, không sắp xếp (nút) | Alpha-Beta + MVV-LVA (nút) | Thời gian Negamax thuần | Thời gian AB + MVV-LVA |
|---|---|---|---|---|---|
| 1 | 45 | 45 | 45 | < 1 ms | < 1 ms |
| 2 | 1.965 | 946 | 171 | 1 ms | 1 ms |
| 3 | 81.631 | 13.250 | 2.298 | 15 ms | 1 ms |
| 4 | 3.371.871 | 100.374 | 26.289 | 437 ms | 11 ms |

**Thế cờ trung cuộc:**

| Độ sâu | Negamax thuần (nút) | Alpha-Beta, không sắp xếp (nút) | Alpha-Beta + MVV-LVA (nút) | Thời gian Negamax thuần | Thời gian AB + MVV-LVA |
|---|---|---|---|---|---|
| 1 | 40 | 40 | 40 | < 1 ms | < 1 ms |
| 2 | 1.633 | 395 | 123 | < 1 ms | < 1 ms |
| 3 | 63.660 | 10.770 | 2.874 | 11 ms | 1 ms |
| 4 | 2.555.146 | 177.741 | 18.882 | 361 ms | 10 ms |

**Nhận xét:**

- Số nút của Negamax thuần tăng xấp xỉ **b^d**: 44^4 ≈ 3,75 triệu so với 3,37 triệu đo được — khớp với phân tích lý thuyết O(b^d).
- Alpha-Beta **không sắp xếp** giảm số nút 14–34 lần ở độ sâu 4.
- Thêm sắp xếp MVV-LVA, số nút giảm tiếp 4–9 lần; tổng cộng giảm **128 lần** (khai cuộc) và **135 lần** (trung cuộc) so với Negamax thuần. Kết quả minh chứng rõ nhận định của Knuth–Moore: *thứ tự nước đi quyết định hiệu quả của Alpha-Beta*.
- Cả ba phiên bản trả về **cùng một giá trị tại gốc** ở mọi độ sâu (khai cuộc: 29, −16, 29, 0; trung cuộc: 75, 44, 78, 43 với d = 1..4) — xác nhận bằng thực nghiệm rằng Alpha-Beta bảo toàn kết quả của Negamax. Sự dao động giá trị giữa độ sâu chẵn và lẻ phản ánh việc bên nào được đi nước cuối trước khi đánh giá.

## 4.3. Engine hoàn chỉnh theo độ sâu

Engine đầy đủ (Negamax + Alpha-Beta + PVS + bảng chuyển vị + null-move + killer/history + tìm kiếm tĩnh + tìm kiếm sâu dần) tại thế cờ khai cuộc. Số nút là **tổng tích luỹ** của tất cả các lần lặp từ độ sâu 1 tới độ sâu d, **bao gồm** cả các nút của tìm kiếm tĩnh:

| Độ sâu d | Tổng số nút | Thời gian | Hệ số tăng so với d − 1 |
|---|---|---|---|
| 1 | 103 | 0,5 ms | — |
| 2 | 978 | 1,2 ms | 9,5 |
| 3 | 5.210 | 2,2 ms | 5,3 |
| 4 | 17.815 | 10,2 ms | 3,4 |
| 5 | 107.807 | 21,5 ms | 6,1 |
| 6 | 234.882 | 49,2 ms | 2,2 |
| 7 | 2.065.015 | 313,8 ms | 8,8 |
| 8 | 7.895.979 | 1.522,9 ms | 3,8 |
| 9 | 53.281.868 | 9.573,4 ms | 6,7 |

**Nhận xét:**

- **Hệ số phân nhánh hiệu dụng** (effective branching factor) từ độ sâu 4 tới 8 là (7.895.979 / 17.815)^(1/4) ≈ **4,6** — so với b ≈ 44 của cây đầy đủ. Mỗi ply thêm vào chỉ tốn khoảng 4–6 lần thay vì 44 lần.
- Hệ số tăng dao động chẵn–lẻ (chuyển lên độ sâu lẻ tốn hơn chuyển lên độ sâu chẵn) là hiện tượng đặc trưng của Alpha-Beta, giải thích được bằng công thức Knuth–Moore b^⌈d/2⌉ + b^⌊d/2⌋ − 1: từ d chẵn lên d + 1, số mũ ⌈d/2⌉ tăng nên chi phí tăng khoảng b/2 lần; từ d lẻ lên d + 1, chỉ số mũ ⌊d/2⌋ tăng nên chi phí chỉ tăng khoảng 2 lần.
- So sánh ở độ sâu 4: engine hoàn chỉnh duyệt 17.815 nút *đã bao gồm* tìm kiếm tĩnh và cả các lần lặp 1–3, ít hơn cả Alpha-Beta + MVV-LVA thuần (26.289 nút) vốn không có tìm kiếm tĩnh.
- Độ sâu 8 đạt trong khoảng 1,5 giây trên máy tính; ước lượng Negamax thuần ở độ sâu 8 cần khoảng 44^8 ≈ 1,4 × 10^13 nút — với tốc độ ≈ 7,7 triệu nút/giây đo ở mục 4.2, tương đương khoảng 3 tuần tính toán liên tục.
- Nước đi được chọn ở mọi độ sâu là Mã (0,1) → (2,2) — "lên Mã", một nước khai cuộc kinh điển.

## 4.4. Hiệu năng theo cấp độ

| Cấp độ | Độ sâu tối đa | Ngân sách | Khai cuộc: số nút | Khai cuộc: thời gian | Trung cuộc: số nút | Trung cuộc: thời gian |
|---|---|---|---|---|---|---|
| Dễ | 4 | 1.500 ms | 17.815 | 9 ms | 23.967 | 16 ms |
| Khó | 6 | 6.000 ms | 234.882 | 56 ms | 247.085 | 61 ms |
| Cực khó | 8 | 24.000 ms | 7.895.979 | 1.552 ms | 5.998.174 | 1.480 ms |

Ở cả ba cấp độ, engine đều **hoàn thành độ sâu tối đa** trong một phần nhỏ của ngân sách thời gian (≤ 6,5%), để lại biên an toàn lớn cho điện thoại chậm hơn máy tính nhiều lần. Trên thiết bị đủ nhanh, điều phân biệt các cấp độ là độ sâu tìm kiếm; đồng hồ chỉ can thiệp khi thiết bị hoặc thế cờ đòi hỏi.

## 4.5. Đánh giá chung

**Kết quả đạt được:**

- Cài đặt thành công Negamax với cắt tỉa Alpha-Beta và 8 kỹ thuật bổ trợ, mỗi kỹ thuật giải quyết một bài toán cụ thể đã phân tích ở Chương 2.
- Số liệu đo xác nhận lý thuyết: Negamax thuần có số nút ≈ b^d; Alpha-Beta với thứ tự tốt giảm hơn 100 lần ở độ sâu 4; engine hoàn chỉnh có hệ số phân nhánh hiệu dụng ≈ 4,6.
- AI tuân thủ đầy đủ luật cờ tướng (bao gồm luật hết nước là thua, tướng không đối mặt, luật lặp thế cờ), trả lời trong thời gian chấp nhận được trên điện thoại, có 3 cấp độ rõ rệt.

**Hạn chế:**

- Hàm đánh giá còn đơn giản (chủ yếu là bảng điểm theo ô), chưa xét tính cơ động, cấu trúc phòng thủ, quân bị ghim.
- Chưa có thư viện khai cuộc và cơ sở dữ liệu tàn cuộc.
- Tìm kiếm chạy đơn luồng, chưa tận dụng nhiều nhân CPU.
- Null-move và cắt delta là các kỹ thuật cắt tỉa *không an toàn tuyệt đối* — trong một số thế hiếm có thể bỏ sót nước tốt.

# CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

## 5.1. Kết luận

Đề tài đã nghiên cứu và áp dụng thuật toán **Negamax** — cách phát biểu gọn của Minimax dựa trên tính chất tổng bằng không — vào trò chơi Cờ Tướng trên nền tảng Android. Báo cáo đã chứng minh tính tương đương giữa Negamax và Minimax, trình bày cắt tỉa Alpha-Beta dưới dạng Negamax với cửa sổ đảo (−β, −α), và phân tích các bài toán phát sinh khi đưa thuật toán vào thực tế: hiệu ứng đường chân trời, thứ tự nước đi, trạng thái lặp, giới hạn thời gian, cửa sổ tìm kiếm, cắt tỉa tiến và zugzwang, khoảng cách chiếu hết, lặp thế cờ.

Điểm mạnh nổi bật của Negamax thể hiện rõ trong quá trình cài đặt: toàn bộ engine xoay quanh **một** hàm đệ quy duy nhất, và mọi kỹ thuật tối ưu chỉ cần viết một lần cho cả hai bên. Số liệu thực nghiệm cho thấy tổ hợp các kỹ thuật này giúp engine đạt độ sâu 8 ply trong khoảng 1,5 giây — điều không thể với Negamax thuần.

## 5.2. Hướng phát triển

1. **Cải thiện hàm đánh giá:** bổ sung tính cơ động, an toàn tướng, cấu trúc Sĩ–Tượng, quân bị ghim; tinh chỉnh trọng số tự động bằng Texel tuning hoặc học tăng cường.
2. **Giảm độ sâu nước muộn (Late Move Reductions)** và **mở rộng khi chiếu (check extension)** để tìm sâu hơn ở các nhánh quan trọng.
3. **Cửa sổ khát vọng (aspiration windows)** tại gốc trong tìm kiếm sâu dần.
4. **Thư viện khai cuộc** và **cơ sở dữ liệu tàn cuộc**.
5. **Tìm kiếm song song** (Lazy SMP) tận dụng CPU đa nhân của điện thoại.
6. Kết hợp **mạng nơ-ron đánh giá (NNUE)** hoặc **Monte Carlo Tree Search** theo hướng của AlphaZero.

# TÀI LIỆU THAM KHẢO

1. S. Russell, P. Norvig, *Artificial Intelligence: A Modern Approach*, 4th ed., Pearson, 2020 — Chương 5: Adversarial Search and Games.
2. C. E. Shannon, "Programming a Computer for Playing Chess", *Philosophical Magazine*, vol. 41, no. 314, 1950.
3. D. E. Knuth, R. W. Moore, "An Analysis of Alpha-Beta Pruning", *Artificial Intelligence*, vol. 6, no. 4, pp. 293–326, 1975.
4. A. L. Zobrist, "A New Hashing Method with Application for Game Playing", Technical Report 88, University of Wisconsin, 1970.
5. A. Reinefeld, "An Improvement to the Scout Tree Search Algorithm", *ICCA Journal*, vol. 6, no. 4, 1983.
6. J. Schaeffer, "The History Heuristic and Alpha-Beta Search Enhancements in Practice", *IEEE Transactions on Pattern Analysis and Machine Intelligence*, vol. 11, no. 11, 1989.
7. L. V. Allis, *Searching for Solutions in Games and Artificial Intelligence*, PhD thesis, University of Limburg, 1994.
8. Chess Programming Wiki — các mục Negamax, Alpha-Beta, Quiescence Search, Transposition Table, Null Move Pruning, Principal Variation Search: https://www.chessprogramming.org
9. Mã nguồn đề tài: https://github.com/khoiln218/ChessAndroid
