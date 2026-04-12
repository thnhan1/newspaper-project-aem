# Feature: MSM (Multi-Site Manager) Setup

## Mục tiêu

Thiết lập cấu trúc Multi-Site Manager cho project `newspaper` hoàn toàn bằng code (JCR XML trong `ui.content`), không cần thao tác thủ công qua Author UI.

## Kiến trúc

```
/content/newspaper/
├── language-masters/      ← Tầng 1: Blueprint nguồn (không publish)
│   ├── en/                ← Blueprint EN (nội dung gốc)
│   ├── vi/                ← Blueprint VI (dịch từ EN)
│   └── de/                ← Blueprint DE (dịch từ EN)
├── vn/                    ← Tầng 2: Region Vietnam
│   ├── en/                ← Live Copy ← language-masters/en
│   └── vi/                ← Live Copy ← language-masters/vi
└── us/                    ← Tầng 2: Region US
    ├── en/                ← Live Copy ← language-masters/en
    └── de/                ← Live Copy ← language-masters/de
```

## MSM Relationship Map

| Blueprint (Nguồn) | Live Copy (Đích) | Khu vực |
|---|---|---|
| `language-masters/en` | `vn/en` | Vietnam - English |
| `language-masters/vi` | `vn/vi` | Vietnam - Tiếng Việt |
| `language-masters/en` | `us/en` | US - English |
| `language-masters/de` | `us/de` | US - Deutsch |

---

## Files Thay Đổi

### Tạo mới

| Full Path | Mục đích | Tác dụng |
|---|---|---|
| `ui.content/src/main/content/jcr_root/content/newspaper/language-masters/.content.xml` | Tạo trang gốc của thư mục `language-masters` | Định nghĩa node `cq:Page` cha cho các Blueprint; khai báo các child `en`, `vi`, `de` để Vault biết cấu trúc con bên dưới |
| `ui.content/src/main/content/jcr_root/content/newspaper/language-masters/en/.content.xml` | Blueprint tiếng Anh — nguồn nội dung chính | Chứa toàn bộ layout + component (teasers, helloworld) di chuyển từ `us/en`; có `cq:language="en"` và mixin `cq:Blueprint` để AEM Sites nhận diện là Blueprint gốc |
| `ui.content/src/main/content/jcr_root/content/newspaper/language-masters/vi/.content.xml` | Blueprint tiếng Việt — nguồn nội dung bản dịch VI | Trang skeleton với `cq:language="vi"` và mixin `cq:Blueprint`; journalist dịch nội dung vào đây trước khi rollout xuống `vn/vi` |
| `ui.content/src/main/content/jcr_root/content/newspaper/language-masters/de/.content.xml` | Blueprint tiếng Đức — nguồn nội dung bản dịch DE | Trang skeleton với `cq:language="de"` và mixin `cq:Blueprint`; nguồn cho Live Copy `us/de` |
| `ui.content/src/main/content/jcr_root/content/newspaper/vn/.content.xml` | Trang gốc region Vietnam | Redirect 302 về `vn/vi`; khai báo child `en` và `vi` cho Vault |
| `ui.content/src/main/content/jcr_root/content/newspaper/vn/en/.content.xml` | Live Copy EN cho region Vietnam | `jcr:content` có mixin `cq:LiveSync` + node con `cq:liveSync` (type `cq:LiveSyncConfig`) với `cq:master` trỏ đến `language-masters/en`; AEM tự đồng bộ nội dung khi rollout |
| `ui.content/src/main/content/jcr_root/content/newspaper/vn/vi/.content.xml` | Live Copy VI cho region Vietnam | Tương tự `vn/en` nhưng `cq:master` trỏ đến `language-masters/vi`; trang này publish cho độc giả VN đọc tiếng Việt |
| `ui.content/src/main/content/jcr_root/content/newspaper/us/de/.content.xml` | Live Copy DE cho region US | `cq:master` trỏ đến `language-masters/de`; phục vụ độc giả US gốc Đức |

### Sửa đổi

| Full Path | Thay đổi | Tác dụng |
|---|---|---|
| `ui.content/src/main/content/jcr_root/content/newspaper/us/en/.content.xml` | Chuyển từ plain `cq:Page` sang Live Copy: xóa toàn bộ content inline, thêm mixin `cq:LiveSync` và node `cq:liveSync` với `cq:master="/content/newspaper/language-masters/en"` | `us/en` không còn giữ nội dung riêng; nội dung đến từ rollout Blueprint EN — đảm bảo single source of truth |
| `ui.content/src/main/content/jcr_root/content/newspaper/us/.content.xml` | Thêm `<de/>` vào danh sách child node | Vault cần biết `de` là child hợp lệ của `us` để deploy đúng cấu trúc JCR |
| `ui.content/src/main/content/jcr_root/content/newspaper/.content.xml` | Thêm `<language-masters/>` và `<vn/>` vào danh sách child node | Vault nhận ra hai sub-tree mới này khi install package; nếu thiếu, Vault sẽ không deploy các trang bên dưới |

---

## Cơ chế hoạt động trong JCR

### Blueprint (`cq:Blueprint` mixin)

```xml
<jcr:content
    jcr:mixinTypes="[cq:Blueprint]"
    cq:language="en"
    .../>
```

Mixin này giúp AEM Sites UI hiển thị icon Blueprint và cho phép dùng "Create Live Copy" wizard từ trang đó.

### Live Copy (`cq:LiveSync` mixin + `cq:LiveSyncConfig` node)

```xml
<jcr:content
    jcr:mixinTypes="[cq:LiveSync]"
    ...>
  <cq:liveSync
      jcr:primaryType="cq:LiveSyncConfig"
      cq:master="/content/newspaper/language-masters/en"
      cq:rolloutConfigs="[/libs/msm/wcm/rolloutconfigs/default]"/>
</jcr:content>
```

| Property | Ý nghĩa |
|---|---|
| `jcr:mixinTypes="[cq:LiveSync]"` | Đánh dấu node là Live Copy; AEM Sites hiển thị chain-link icon |
| `cq:liveSync` (node) | Chứa config MSM relationship cho page này |
| `cq:master` | Path tuyệt đối đến Blueprint nguồn |
| `cq:rolloutConfigs` | Chính sách rollout — `default` đồng bộ cả structure lẫn content |

---

## Lưu ý vận hành

- **`language-masters/` không được publish**: Cần exclude path này trong cấu hình Replication Agent trên Author instance (không phải trong code).
- **Rollout flow**: Sửa nội dung tại `language-masters/en` → chọn Rollout → `vn/en` và `us/en` nhận bản cập nhật.
- **Bài chưa dịch**: Nếu `language-masters/vi` chưa có bài nào đó, `vn/vi` sẽ không có trang đó — không tự fallback; cần redirect về `vn/en` hoặc trả 404 theo logic điều hướng riêng.
- **Override region**: Live Copy có thể override một số field (ví dụ giá quảng cáo, pháp lý địa phương) bằng cách cancel inheritance trên property đó trong AEM Sites.


## Vấn đề config

Câu hỏi rất hay — có hai vấn đề tách biệt cần làm rõ.

---

## 1. Cách tạo cấu trúc MSM bằng JCR XML — đúng chuẩn chưa?

**Đúng về kỹ thuật, nhưng chưa đủ cho production thực tế.** Đây là sự khác biệt:

### Vấn đề cốt lõi: Package install ≠ MSM API

Khi Author dùng UI tạo LiveCopy, AEM gọi internal `LiveRelationshipManager.create()` API, nó làm **nhiều hơn** việc chỉ set mixin + node:

1. Copy toàn bộ content từ Blueprint xuống LiveCopy
2. Register relationship vào MSM tracking system
3. Set thêm các metadata properties (`cq:sourceLastModified`, `msm:*`, v.v.)
4. Thiết lập inheritance tracking cho từng property

Khi deploy bằng package (như cách hiện tại), AEM chỉ nhận JCR nodes — **MSM relationship tồn tại trên giấy nhưng chưa được "kích hoạt"** qua API. Lần rollout đầu tiên từ Author mới thực sự sync đúng.

### Mức độ phù hợp theo môi trường

| Môi trường | Cách hiện tại có phù hợp? | Lý do |
|---|---|---|
| **Local dev / fresh AEM** | Tốt | Dev mới clone repo, chạy package, có ngay skeleton để rollout thử |
| **Team sharing qua Git** | Tốt | Đây đúng là mục đích của `ui.content` — seed content có version control |
| **CI/CD pipeline (DEV/STAGE)** | Được | Deploy package tạo structure, sau đó trigger rollout qua HTTP API |
| **Production** | Không nên | Author content không commit vào code; MSM relationship nên được Author tạo và quản lý trong AEM |

### Chuẩn Adobe cho production

Adobe khuyến nghị flow sau:

```
Developer (code)                     Author (AEM UI)
─────────────────                    ──────────────────────────
ui.content chứa:                     Lần đầu setup:
- /conf/newspaper (templates)    →   1. Tạo language-masters pages
- /content/newspaper (root)      →   2. Create Live Copy qua Sites console
  (chỉ đến level site root,           (AEM gọi API đúng cách)
   không tạo sẵn LiveCopy)       →   3. Rollout lần đầu
                                      Sau đó: Author quản lý content
```

### Cách tốt hơn nếu vẫn muốn full code

Dùng **Groovy script chạy một lần** (qua AEM Groovy Console của ACS Tools) thay vì raw XML. Script gọi đúng MSM Java API:

```groovy
import com.day.cq.wcm.msm.api.LiveRelationshipManager
import com.day.cq.wcm.msm.api.RolloutConfig

def lrm = sling.getService(LiveRelationshipManager)
def pageManager = resourceResolver.adaptTo(PageManager)

def blueprint = pageManager.getPage("/content/newspaper/language-masters/en")
def rolloutConfigs = ["/libs/msm/wcm/rolloutconfigs/default"] as String[]

lrm.establishRelationship(
    resourceResolver,
    blueprint.adaptTo(Resource),
    "/content/newspaper/vn/en",
    true,   // deep copy
    rolloutConfigs
)

resourceResolver.commit()
```

Script này chạy một lần sau deploy, đảm bảo MSM relationship đúng chuẩn như Author tạo qua UI.

---

## 2. Blueprint — phải tạo thủ công bằng UI hay code tạo được rồi?

**Code tạo được rồi, và đúng.**

Mixin `cq:Blueprint` mà tôi thêm vào `jcr:content` của các language-master pages:

```xml
<jcr:content
    jcr:mixinTypes="[cq:Blueprint]"
    cq:language="en"
    .../>
```

Đây chính xác là những gì AEM lưu vào JCR khi bạn dùng Author UI. Không cần tạo thêm gì bằng UI cho phần này.

**Tuy nhiên có một điểm cần lưu ý**: Trong AEM có thêm khái niệm **Blueprint Configuration** lưu tại `/etc/blueprints/<name>` (dạng cũ, pre-6.3) hoặc qua `cq:BlueprintConfig` node. Với AEM 6.5 hiện đại, chỉ cần mixin `cq:Blueprint` trên page là đủ để:
- Rollout hoạt động
- Sites admin hiển thị Blueprint icon
- LiveCopy wizard nhận ra Blueprint source

Blueprint Configuration riêng chỉ cần thiết nếu bạn dùng **Blueprint Console** (`/libs/wcm/msm/gui/content/blueprints.html`) để quản lý centrally — không bắt buộc cho MSM cơ bản.

---

## Tóm lại

| Câu hỏi | Câu trả lời |
|---|---|
| Code hiện tại chia sẻ được cho team qua Git không? | **Có** — đây đúng là mục đích của `ui.content` |
| Đủ chuẩn production không? | **Chưa** — production nên để Author tạo LiveCopy qua UI hoặc dùng Groovy script gọi MSM API |
| `cq:Blueprint` mixin cần tạo thêm bằng UI không? | **Không** — code XML đã đủ |
| Cần làm gì thêm sau khi deploy lên AEM? | Vào Sites admin, chọn `language-masters/en` → **Rollout** để AEM kích hoạt relationship đúng cách |

## Site và Page

Câu hỏi rất sắc. Cần phân biệt hai khái niệm "site" trong AEM.

---

## "Site" trong AEM có hai nghĩa khác nhau

### Nghĩa 1 — AEM Sites Console (UI)

Trong Sites console, một node hiển thị như "Site" (có globe icon) khi nó có property `cq:conf` trỏ đến `/conf/<name>`. Đây là **Sling Context-Aware Configuration** — giúp page biết dùng templates/policies nào.

Nhìn vào structure hiện tại:

```
/content/newspaper/          ← có cq:conf="/conf/newspaper" → hiển thị như Site root
├── language-masters/        ← KHÔNG có cq:conf → chỉ là Page
├── vn/                      ← KHÔNG có cq:conf → chỉ là Page
└── us/                      ← KHÔNG có cq:conf → chỉ là Page
```

`us/` và `vn/` hiện là **content page thông thường**, không phải Site root riêng. Nhưng điều này **không sai** vì chúng kế thừa `cq:conf` từ `/content/newspaper` qua Sling CA Config.

---

### Nghĩa 2 — MSM "Live Copy Site"

Trong MSM documentation, "site" = **toàn bộ subtree Live Copy**. Nghĩa này không liên quan đến `cq:conf` — chỉ cần có `cq:LiveSync` mixin là trang đó là "Live Copy site" trong MSM terminology.

Theo nghĩa này, `us/en` và `vn/vi` đã là Live Copy site đúng rồi.

---

## Chuẩn production thực tế trông như thế nào?

Có **hai mô hình**, Adobe support cả hai:

### Mô hình A — Shared conf (đơn giản, phổ biến)
```
/conf/newspaper/              ← một conf dùng chung cho toàn site
/content/newspaper/           ← cq:conf="/conf/newspaper"
  ├── language-masters/en/    ← inherit conf từ root
  ├── vn/en/                  ← inherit conf từ root
  └── us/en/                  ← inherit conf từ root
```
Các region dùng cùng templates/policies → đây là cách project `newspaper` đang làm, và **phù hợp cho mock project**.

### Mô hình B — Separate conf per region (phức tạp hơn, cho enterprise)
```
/conf/newspaper-vn/           ← templates/policies riêng cho VN
/conf/newspaper-us/           ← templates/policies riêng cho US
/content/newspaper/
  ├── vn/    ← cq:conf="/conf/newspaper-vn" → hiện như Site riêng
  └── us/    ← cq:conf="/conf/newspaper-us" → hiện như Site riêng
```
Dùng khi mỗi region cần template khác nhau (ví dụ VN có template "tin nóng", US không có).

---

## Kết luận

| Câu hỏi | Trả lời |
|---|---|
| `us/`, `vn/` hiện là gì? | `cq:Page` thông thường, không có conf riêng |
| Có phải tạo thành "Site" riêng không? | **Không bắt buộc** — chúng inherit `cq:conf` từ `/content/newspaper` |
| MSM chuẩn cần `us/`, `vn/` là Site riêng không? | Không — MSM quan tâm đến `cq:LiveSync` mixin, không phải `cq:conf` |
| Khi nào mới cần Mô hình B? | Khi VN và US cần **bộ templates hoàn toàn khác nhau**, hoặc cần phân quyền author riêng biệt theo region |

Với `newspaper` là mock/learning project, Mô hình A (shared conf) hoàn toàn đúng chuẩn và đủ.