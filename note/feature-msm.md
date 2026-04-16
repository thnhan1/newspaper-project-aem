# Feature: MSM (Multi-Site Manager) — Implementation Summary

## Kiến trúc tổng quan

```
/content/newspaper/                         ← Site root (cq:conf, cq:allowedTemplates)
├── language-masters/                       ← Container page (Blueprint source)
│   ├── en/                                 ← Language root EN + sections + subsections
│   ├── vi/                                 ← Language root VI + sections
│   └── de/                                 ← Language root DE + sections
└── us/                                     ← Region container (giữ lại từ archetype gốc)

/content/cq:tags/newspaper/                 ← Tag namespace
├── sections/   (news, opinion, sport, culture, lifestyle)
└── subsections/ (top-stories, world, politics, ... 21 tags)

/apps/newspaper/blueprintconfigs/newspaper  ← Blueprint Configuration
```

User tự tạo Live Copy qua AEM Author UI:
```
language-masters/en  ──Create Live Copy──►  vn/en, us/en
language-masters/vi  ──Create Live Copy──►  vn/vi
language-masters/de  ──Create Live Copy──►  us/de
```

---

## Tất cả files đã tạo / sửa

### 1. Blueprint Configuration (ui.apps)

| File | Mục đích | Tác dụng |
|---|---|---|
| `ui.apps/.../apps/newspaper/blueprintconfigs/.content.xml` | Đăng ký Blueprint Configuration cho AEM | AEM nhận ra `language-masters` là Blueprint source; wizard "Create → Site from Blueprint" hiển thị "Newspaper" cho user chọn, deep copy toàn bộ tree khi tạo Live Copy site |
| `ui.apps/.../META-INF/vault/filter.xml` | Thêm filter `/apps/newspaper/blueprintconfigs` | Vault deploy Blueprint Config lên AEM khi install package |

### 2. Tags — Namespace + Sections + Subsections (ui.content)

| File | Mục đích | Tác dụng |
|---|---|---|
| `ui.content/.../content/_cq_tags/newspaper/.content.xml` | Tag namespace `newspaper` | Tạo root cho tất cả tags của project; khai báo child `sections` và `subsections` |
| `ui.content/.../content/_cq_tags/newspaper/sections/.content.xml` | 5 section tags | Định nghĩa `news`, `opinion`, `sport`, `culture`, `lifestyle` với i18n (EN/VI/DE); dùng gắn vào section pages |
| `ui.content/.../content/_cq_tags/newspaper/subsections/.content.xml` | 21 subsection tags | Định nghĩa `top-stories`, `world`, `politics`, `business`, `technology`, `science`, `health`, `education`, `environment`, `features`, `in-focus`, `exclusive`, `interviews`, `analysis`, `editorials`, `cartoon`, `food`, `fashion`, `travel`, `relationships`, `fitness` với i18n (EN/VI/DE) |

### 3. Language Masters — Blueprint Pages (ui.content)

| File | Mục đích | Tác dụng |
|---|---|---|
| `ui.content/.../content/newspaper/language-masters/.content.xml` | Container page cho tất cả language masters | Trang cha chứa `en`, `vi`, `de`; không có `cq:conf` (kế thừa từ site root) |
| `ui.content/.../content/newspaper/language-masters/en/.content.xml` | Language root EN — Blueprint chính | Có `cq:conf`, `cq:allowedTemplates`, `cq:language="en"`; là nguồn nội dung gốc cho toàn site; khai báo 5 section children |
| `ui.content/.../content/newspaper/language-masters/vi/.content.xml` | Language root VI | Tương tự EN với `cq:language="vi"`; journalist dịch bài vào đây; khai báo 5 section children |
| `ui.content/.../content/newspaper/language-masters/de/.content.xml` | Language root DE | Tương tự EN với `cq:language="de"`; khai báo 5 section children |

### 4. EN Section Pages (ui.content)

| File | Mục đích | Tác dụng |
|---|---|---|
| `.../language-masters/en/news/.content.xml` | Section "News" | `cq:tags="[newspaper:sections/news]"`; khai báo 9 subsection children |
| `.../language-masters/en/opinion/.content.xml` | Section "Opinion" | `cq:tags="[newspaper:sections/opinion]"`; khai báo 7 subsection children |
| `.../language-masters/en/sport/.content.xml` | Section "Sport" | `cq:tags="[newspaper:sections/sport]"` |
| `.../language-masters/en/culture/.content.xml` | Section "Culture" | `cq:tags="[newspaper:sections/culture]"` |
| `.../language-masters/en/lifestyle/.content.xml` | Section "Lifestyle" | `cq:tags="[newspaper:sections/lifestyle]"`; khai báo 5 subsection children |

### 5. EN Subsection Pages — News (ui.content)

| File | Tag |
|---|---|
| `.../en/news/top-stories/.content.xml` | `newspaper:subsections/top-stories` |
| `.../en/news/world/.content.xml` | `newspaper:subsections/world` |
| `.../en/news/politics/.content.xml` | `newspaper:subsections/politics` |
| `.../en/news/business/.content.xml` | `newspaper:subsections/business` |
| `.../en/news/technology/.content.xml` | `newspaper:subsections/technology` |
| `.../en/news/science/.content.xml` | `newspaper:subsections/science` |
| `.../en/news/health/.content.xml` | `newspaper:subsections/health` |
| `.../en/news/education/.content.xml` | `newspaper:subsections/education` |
| `.../en/news/environment/.content.xml` | `newspaper:subsections/environment` |

### 6. EN Subsection Pages — Opinion (ui.content)

| File | Tag |
|---|---|
| `.../en/opinion/features/.content.xml` | `newspaper:subsections/features` |
| `.../en/opinion/in-focus/.content.xml` | `newspaper:subsections/in-focus` |
| `.../en/opinion/exclusive/.content.xml` | `newspaper:subsections/exclusive` |
| `.../en/opinion/interviews/.content.xml` | `newspaper:subsections/interviews` |
| `.../en/opinion/analysis/.content.xml` | `newspaper:subsections/analysis` |
| `.../en/opinion/editorials/.content.xml` | `newspaper:subsections/editorials` |
| `.../en/opinion/cartoon/.content.xml` | `newspaper:subsections/cartoon` |

### 7. EN Subsection Pages — Lifestyle (ui.content)

| File | Tag |
|---|---|
| `.../en/lifestyle/food/.content.xml` | `newspaper:subsections/food` |
| `.../en/lifestyle/fashion/.content.xml` | `newspaper:subsections/fashion` |
| `.../en/lifestyle/travel/.content.xml` | `newspaper:subsections/travel` |
| `.../en/lifestyle/relationships/.content.xml` | `newspaper:subsections/relationships` |
| `.../en/lifestyle/fitness/.content.xml` | `newspaper:subsections/fitness` |

### 8. VI & DE Section Pages (ui.content)

| File | Title |
|---|---|
| `.../language-masters/vi/news/.content.xml` | Tin tức |
| `.../language-masters/vi/opinion/.content.xml` | Quan điểm |
| `.../language-masters/vi/sport/.content.xml` | Thể thao |
| `.../language-masters/vi/culture/.content.xml` | Văn hóa |
| `.../language-masters/vi/lifestyle/.content.xml` | Đời sống |
| `.../language-masters/de/news/.content.xml` | Nachrichten |
| `.../language-masters/de/opinion/.content.xml` | Meinung |
| `.../language-masters/de/sport/.content.xml` | Sport |
| `.../language-masters/de/culture/.content.xml` | Kultur |
| `.../language-masters/de/lifestyle/.content.xml` | Lebensstil |

### 9. Sửa đổi files có sẵn

| File | Thay đổi | Tác dụng |
|---|---|---|
| `ui.content/.../content/newspaper/.content.xml` | Thêm `<language-masters/>` vào child references | Vault biết deploy sub-tree `language-masters` bên dưới site root |
| `ui.content/.../content/newspaper/us/.content.xml` | Giữ nguyên — region container gốc archetype | User tự tạo Live Copy `us/en`, `us/de` bằng Author UI |
| `ui.content/.../META-INF/vault/filter.xml` | Thêm filter `/content/cq:tags/newspaper` | Deploy tags khi install package |

---

## Vault Filter tổng hợp

### ui.apps/filter.xml
```xml
<filter root="/apps/newspaper/clientlibs"/>
<filter root="/apps/newspaper/components"/>
<filter root="/apps/newspaper/i18n"/>
<filter root="/apps/newspaper/blueprintconfigs"/>    ← MỚI
```

### ui.content/filter.xml
```xml
<filter root="/content/cq:tags/newspaper" mode="merge"/>  ← MỚI
<filter root="/conf/newspaper" mode="merge"/>
<filter root="/content/newspaper" mode="merge"/>
<filter root="/content/dam/newspaper" mode="merge">...</filter>
<filter root="/content/dam/newspaper/asset.jpg" mode="merge"/>
<filter root="/content/experience-fragments/newspaper" mode="merge"/>
```

---

## Cách sử dụng sau khi deploy

### Tạo Live Copy site mới (ví dụ: Vietnam)

1. Sites console → `/content/newspaper/language-masters`
2. Chọn `en` → **Create → Live Copy**
3. Destination: `/content/newspaper/vn`, Name: `en`
4. **✅ Include sub pages** → Create
5. Lặp lại cho `vi` → destination `/content/newspaper/vn`, Name: `vi`

### Rollout khi có thay đổi

1. Sửa nội dung tại `language-masters/en/news/...`
2. Chọn page → toolbar → **References** → **Live Copies** → **Rollout**
3. Tất cả Live Copies (`vn/en/news/...`, `us/en/news/...`) nhận update

---

## Thống kê

| Loại | Số lượng |
|---|---|
| Files tạo mới | ~45 |
| Files sửa đổi | 3 |
| Tags (sections) | 5 |
| Tags (subsections) | 21 |
| Language masters | 3 (en, vi, de) |
| EN section pages | 5 |
| EN subsection pages | 21 |
| VI/DE section pages | 10 (5 mỗi ngôn ngữ) |
| Blueprint Configuration | 1 |
