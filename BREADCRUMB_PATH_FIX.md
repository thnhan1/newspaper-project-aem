# Breadcrumb Path Fix - Chi Tiết Vấn Đề và Giải Pháp

## 🐛 Vấn Đề Phát Hiện

### Triệu Chứng
Breadcrumb chỉ hiển thị: **Home / bai-1**  
Mặc dù article có tag `newspaper:news/tech`

### Nguyên Nhân Chính

#### 1. Đọc Tags Từ Sai Vị Trí (ĐÃ FIX LẦN 1)

**❌ Code Cũ:**
```java
ValueMap pageProperties = currentPage.getProperties();
String[] tags = pageProperties.get("cq:tags", String[].class);
```

**Vấn đề:** Đọc từ component resource thay vì jcr:content

**✅ Fix:**
```java
Resource contentResource = currentPage.getContentResource();
ValueMap pageProperties = contentResource.getValueMap();
String[] tags = pageProperties.get("cq:tags", String[].class);
```

#### 2. Sai Depth Khi Lấy Root Page (VẤN ĐỀ CHÍNH - ĐÃ FIX LẦN 2)

**❌ Code Cũ:**
```java
private static final int ROOT_DEPTH = 4;
Page rootPage = currentPage.getAbsoluteParent(ROOT_DEPTH);
```

**Phân Tích Depth:**

Với article path: `/content/newspaper/language-masters/en/articles/2024/04/15/bai-1`

```
Depth 0: /content
Depth 1: /content/newspaper
Depth 2: /content/newspaper/language-masters
Depth 3: /content/newspaper/language-masters/en          ← LANGUAGE ROOT (đúng)
Depth 4: /content/newspaper/language-masters/en/articles ← SAI!
```

**Kết quả:**
- Code cũ lấy depth 4 = `/content/.../en/articles`
- Khi ghép với tag `news/tech`:
  - Path tìm kiếm: `/content/.../en/articles/news` ❌ SAI
  - Path đúng phải là: `/content/.../en/news` ✅

**✅ Fix:**
```java
private static final int LANGUAGE_ROOT_DEPTH = 3; // /content/newspaper/language-masters/en
Page languageRoot = currentPage.getAbsoluteParent(LANGUAGE_ROOT_DEPTH);
```

### Logic Mới (Đúng)

```java
private void buildBreadcrumbItems() {
    // Lấy language root tại depth 3
    Page languageRoot = currentPage.getAbsoluteParent(LANGUAGE_ROOT_DEPTH);
    
    // Add Home (language root)
    items.add(new BreadcrumbItem(languageRoot, false, linkManager, true));
    
    // Extract tag path: "newspaper:news/tech" → "news/tech"
    String tagPath = extractTagPath();
    
    if (StringUtils.isNotBlank(tagPath)) {
        String[] segments = tagPath.split("/"); // ["news", "tech"]
        String basePath = languageRoot.getPath(); // /content/.../en
        
        for (String segment : segments) {
            // Build đúng: /content/.../en/news, /content/.../en/news/tech
            String topicPath = basePath + "/" + segment;
            Page topicPage = pageManager.getPage(topicPath);
            
            if (topicPage != null) {
                items.add(new BreadcrumbItem(topicPage, false, linkManager, false));
            }
            
            // Update basePath cho nested segments
            basePath = topicPath;
        }
    }
    
    // Add current article
    items.add(new BreadcrumbItem(currentPage, true, linkManager, false));
}
```

## 📊 So Sánh Trước và Sau

### Trước Fix

```
Article Path: /content/newspaper/language-masters/en/articles/2024/04/15/bai-1
Tag:          newspaper:news/tech

Code tìm kiếm tại:
1. /content/newspaper/language-masters/en/articles        (Home - depth 4)
2. /content/newspaper/language-masters/en/articles/news   (❌ Không tồn tại)
3. /content/newspaper/language-masters/en/articles/news/tech (❌ Không tồn tại)

Kết quả: Home / bai-1
```

### Sau Fix

```
Article Path: /content/newspaper/language-masters/en/articles/2024/04/15/bai-1
Tag:          newspaper:news/tech

Code tìm kiếm tại:
1. /content/newspaper/language-masters/en                (Home - depth 3)
2. /content/newspaper/language-masters/en/news           (✅ Landing Page)
3. /content/newspaper/language-masters/en/news/tech      (✅ Listing Page)

Kết quả: Home / News / Tech / bai-1
```

## 🔍 Debugging Logs

Sau khi deploy, kiểm tra logs tại:
- **AEM Console:** http://localhost:4502/system/console/slinglog
- **Log File:** `/crx-quickstart/logs/error.log`

### Logs Mẫu (Thành Công)

```
INFO [BreadcrumbModel] Language root found: /content/newspaper/language-masters/en
INFO [BreadcrumbModel] Found tags on page /content/.../bai-1: [newspaper:news/tech]
INFO [BreadcrumbModel] Using tag 'newspaper:news/tech' for breadcrumb, extracted path: news/tech
INFO [BreadcrumbModel] Building breadcrumb from tag path: news/tech
DEBUG [BreadcrumbModel] Looking for topic/subtopic page at path: /content/.../en/news
INFO [BreadcrumbModel] Found topic page: /content/.../en/news (title: News, hideInNav: false)
DEBUG [BreadcrumbModel] Added breadcrumb item: News
DEBUG [BreadcrumbModel] Looking for topic/subtopic page at path: /content/.../en/news/tech
INFO [BreadcrumbModel] Found topic page: /content/.../en/news/tech (title: Technology, hideInNav: false)
DEBUG [BreadcrumbModel] Added breadcrumb item: Technology
DEBUG [BreadcrumbModel] Added current page to breadcrumb: bai-1
INFO [BreadcrumbModel] Built breadcrumb with 4 items for page: /content/.../bai-1
```

### Logs Mẫu (Topic Page Không Tồn Tại)

```
INFO [BreadcrumbModel] Language root found: /content/newspaper/language-masters/en
INFO [BreadcrumbModel] Found tags on page /content/.../bai-1: [newspaper:news/tech]
INFO [BreadcrumbModel] Using tag 'newspaper:news/tech' for breadcrumb, extracted path: news/tech
INFO [BreadcrumbModel] Building breadcrumb from tag path: news/tech
DEBUG [BreadcrumbModel] Looking for topic/subtopic page at path: /content/.../en/news
WARN [BreadcrumbModel] Topic/subtopic page not found at path: /content/.../en/news - skipping this segment
DEBUG [BreadcrumbModel] Looking for topic/subtopic page at path: /content/.../en/news/tech
WARN [BreadcrumbModel] Topic/subtopic page not found at path: /content/.../en/news/tech - skipping this segment
DEBUG [BreadcrumbModel] Added current page to breadcrumb: bai-1
INFO [BreadcrumbModel] Built breadcrumb with 2 items for page: /content/.../bai-1
```

## ✅ Cách Test

### 1. Refresh Page
- Mở article page trong browser
- Hard refresh: `Ctrl + F5` (Windows) hoặc `Cmd + Shift + R` (Mac)

### 2. Kiểm Tra Breadcrumb
Nếu có đủ pages, sẽ hiển thị:
```
Home > News > Tech > bai-1
```

### 3. Kiểm Tra Topic Pages Có Tồn Tại

Vào CRXDE Lite: http://localhost:4502/crx/de/index.jsp

Kiểm tra cấu trúc:
```
/content
  /newspaper
    /language-masters
      /en
        /news (jcr:primaryType=cq:Page)          ← Cần có
          /tech (jcr:primaryType=cq:Page)        ← Cần có
        /articles
          /2024
            /04
              /15
                /bai-1 (jcr:primaryType=cq:Page)
```

### 4. Tạo Missing Pages (Nếu Cần)

#### Cách 1: Trong AEM Sites Console
1. Mở: http://localhost:4502/sites.html/content/newspaper/language-masters/en
2. Click **Create** → **Page**
3. Chọn template (ví dụ: Content Page)
4. Title: "News", Name: "news"
5. Create
6. Lặp lại cho "tech" page bên trong "news"

#### Cách 2: Qua CRXDE Lite (Nhanh hơn cho testing)
1. Mở: http://localhost:4502/crx/de/index.jsp
2. Right-click `/content/newspaper/language-masters/en`
3. **Create** → **Create Node**
   - Name: `news`
   - Type: `cq:Page`
4. Right-click node `news` vừa tạo
5. **Create** → **Create Node**
   - Name: `jcr:content`
   - Type: `cq:PageContent`
6. Select node `jcr:content`, add properties:
   - `jcr:title` (String): "News"
   - `sling:resourceType` (String): "newspaper/components/page"
7. Click **Save All**
8. Lặp lại cho `tech` page

## 📝 Files Đã Thay Đổi

### core/src/main/java/com/fa/core/models/BreadcrumbModel.java

**Thay đổi:**
1. Đổi constant: `ROOT_DEPTH` → `LANGUAGE_ROOT_DEPTH = 3`
2. Fix `extractTagPath()`: Đọc từ `contentResource.getValueMap()`
3. Fix `buildBreadcrumbItems()`: Dùng `languageRoot.getPath()` làm base path
4. Fix `fallbackToSimpleBreadcrumb()`: Dùng `LANGUAGE_ROOT_DEPTH`
5. Thêm detailed logging cho debugging

## 🚀 Deploy

Code đã được deploy thành công:
```bash
cd c:\dev\newspaper\core
mvn clean install -PautoInstallBundle
```

**Status:** ✅ BUILD SUCCESS  
**Bundle installed:** http://localhost:4502/system/console/bundles

## 🎯 Expected Result

Với article có tag `newspaper:news/tech`:

**Breadcrumb sẽ hiển thị:**
```
🏠 Home > News > Tech > bai-1
```

Trong đó:
- **Home** → `/content/newspaper/language-masters/en`
- **News** → `/content/newspaper/language-masters/en/news`
- **Tech** → `/content/newspaper/language-masters/en/news/tech`
- **bai-1** → `/content/newspaper/language-masters/en/articles/.../bai-1` (current)

## 💡 Notes

1. **Nếu topic pages không tồn tại:** Breadcrumb sẽ skip và chỉ show: `Home > bai-1`

2. **Multi-level tags:** Code hỗ trợ tag có nhiều cấp:
   - `newspaper:news/tech/ai` → Home > News > Tech > AI > Article

3. **Hidden pages:** Nếu topic page có `hideInNav=true`, sẽ bị skip (trừ khi `showHidden=true` trong dialog)

4. **Multi-language:** Code tự động detect language root:
   - `/content/newspaper/language-masters/en` (English)
   - `/content/newspaper/language-masters/vi` (Vietnamese)
   - `/content/newspaper/vn/vi` (Vietnam region)

## 🔧 Troubleshooting

### Vẫn chỉ hiển thị "Home / bai-1"

**Kiểm tra:**
1. ✅ Bundle đã deploy? → http://localhost:4502/system/console/bundles (search "newspaper.core")
2. ✅ Tag có đúng format? → Mở page properties, check `cq:tags = ["newspaper:news/tech"]`
3. ✅ Topic pages có tồn tại? → Vào CRXDE check `/content/.../en/news` và `/content/.../en/news/tech`
4. ✅ Xem logs → `/crx-quickstart/logs/error.log` tìm "[BreadcrumbModel]"

### Breadcrumb không hiển thị gì cả

**Kiểm tra:**
1. Component đã add vào page? → Vào Edit mode, check breadcrumb component
2. Page có nằm trong `/articles/`? → Code chỉ chạy cho article pages
3. XDebug HTL template → Thêm `${breadcrumb.items.size}` vào template để test

---

**Last Updated:** April 15, 2026  
**Version:** 1.0.1  
**Status:** ✅ Fixed and Deployed
