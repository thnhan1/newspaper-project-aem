# Breadcrumb Component - Final Version

## 📋 Overview

Breadcrumb component đã được implement với **path-based navigation** phù hợp với MSM (Multi-Site Manager) structure.

**Lý do:** Khi dùng MSM, tất cả ngôn ngữ đều dùng chung cấu trúc URL (`news/tech`), không có URL dịch.

## 🏗️ Content Structure (MSM)

```
/content/newspaper/
├── language-masters/
│   ├── en/
│   │   ├── news/                    
│   │   │   └── tech/                
│   │   └── articles/
│   │       └── 2024/04/15/
│   │           └── article-1        (cq:tags = ["newspaper:news/tech"])
│   │
│   └── vi/                          (MSM Live Copy từ en/)
│       ├── news/                    ← Same URL structure!
│       │   └── tech/                
│       └── articles/
│           └── 2024/04/15/
│               └── bai-viet-1       (cq:tags = ["newspaper:news/tech"])
│
└── vn/
    └── vi/                          (MSM Live Copy)
        ├── news/                    ← Same URL structure!
        │   └── tech/
        └── articles/...
```

## 🔧 Implementation Logic

### 1. Extract Tag từ Article

```java
// Article: /content/.../en/articles/2024/04/15/article-1
// cq:tags: ["newspaper:news/tech"]

String tagPath = extractTagPath(); // Returns: "news/tech"
```

### 2. Build Path từ Language Root

```java
// Language root: /content/newspaper/language-masters/en
// Tag path: "news/tech"

String[] segments = tagPath.split("/"); // ["news", "tech"]

for (String segment : segments) {
    String topicPath = languageRoot.getPath() + "/" + segment;
    // Loop 1: /content/.../en/news
    // Loop 2: /content/.../en/news/tech
    
    Page topicPage = pageManager.getPage(topicPath);
    if (topicPage != null) {
        items.add(new BreadcrumbItem(topicPage, false, linkManager, false));
    }
}
```

### 3. Result

```
English Article:
Path: /content/.../en/articles/2024/04/15/article-1
Tag: newspaper:news/tech
Breadcrumb: Home > News > Tech > Article 1

Vietnamese Article (MSM Live Copy):
Path: /content/.../vi/articles/2024/04/15/bai-viet-1
Tag: newspaper:news/tech
Breadcrumb: Home > News > Tech > Bài viết 1
         (Same structure, Vietnamese titles from page properties)
```

## ✅ Key Features

### 1. Automatic Tag Extraction
- Đọc `cq:tags` từ article page (`jcr:content` node)
- Parse tag: `newspaper:news/tech` → `news/tech`
- Build breadcrumb path từ language root

### 2. Multi-level Support
- Single level: `newspaper:news` → Home > News
- Multi-level: `newspaper:news/tech` → Home > News > Tech
- Deep nesting: `newspaper:news/tech/ai` → Home > News > Tech > AI

### 3. Fallback Handling
- Nếu không có tags: Show `Home > Article`
- Nếu topic page không tồn tại: Skip và show `Home > Article`
- Nếu topic page hidden: Skip (trừ khi `showHidden=true`)

### 4. MSM Compatibility
- Tất cả language copies dùng chung structure
- Breadcrumb titles tự động lấy từ page properties (i18n)
- Không cần config riêng cho từng ngôn ngữ

## 📊 Example Flow

### English Article

```
Article Path: /content/newspaper/language-masters/en/articles/2024/04/15/tech-innovation
Article Tags: ["newspaper:news/tech"]

Language Root (depth 3): /content/newspaper/language-masters/en

Build Process:
1. Add Home: /content/.../en (title: "Home")
2. Extract tag: "news/tech" → split to ["news", "tech"]
3. Find news page: /content/.../en/news (title: "News")
4. Find tech page: /content/.../en/news/tech (title: "Technology")
5. Add current: /content/.../en/articles/.../tech-innovation (title: "Tech Innovation")

Result: Home > News > Technology > Tech Innovation
```

### Vietnamese Article (MSM Live Copy)

```
Article Path: /content/newspaper/language-masters/vi/articles/2024/04/15/cai-tien-cong-nghe
Article Tags: ["newspaper:news/tech"] (inherited from master)

Language Root (depth 3): /content/newspaper/language-masters/vi

Build Process:
1. Add Home: /content/.../vi (title: "Trang chủ")
2. Extract tag: "news/tech" → split to ["news", "tech"]
3. Find news page: /content/.../vi/news (title: "Tin tức")
4. Find tech page: /content/.../vi/news/tech (title: "Công nghệ")
5. Add current: /content/.../vi/articles/.../cai-tien-cong-nghe (title: "Cải tiến công nghệ")

Result: Trang chủ > Tin tức > Công nghệ > Cải tiến công nghệ
```

## 🔍 Debug Logs

```
INFO [BreadcrumbModel] Language root found: /content/newspaper/language-masters/en
INFO [BreadcrumbModel] Found tags on page: [newspaper:news/tech]
INFO [BreadcrumbModel] Using tag 'newspaper:news/tech', extracted path: news/tech
INFO [BreadcrumbModel] Building breadcrumb from tag path: news/tech
DEBUG [BreadcrumbModel] Looking for topic/subtopic page at path: .../en/news
INFO [BreadcrumbModel] Found topic page: .../en/news (title: News, hideInNav: false)
DEBUG [BreadcrumbModel] Added breadcrumb item: News
DEBUG [BreadcrumbModel] Looking for topic/subtopic page at path: .../en/news/tech
INFO [BreadcrumbModel] Found topic page: .../en/news/tech (title: Technology, hideInNav: false)
DEBUG [BreadcrumbModel] Added breadcrumb item: Technology
DEBUG [BreadcrumbModel] Added current page to breadcrumb: Tech Innovation
INFO [BreadcrumbModel] Built breadcrumb with 4 items for page: .../tech-innovation
```

## 🎯 Setup Requirements

### 1. Create Topic Pages

```
/content/newspaper/language-masters/en/
├── news/                   (jcr:title = "News")
│   └── tech/               (jcr:title = "Technology")
└── articles/...

/content/newspaper/language-masters/vi/  (MSM Live Copy)
├── news/                   (jcr:title = "Tin tức")
│   └── tech/               (jcr:title = "Công nghệ")
└── articles/...
```

### 2. Tag Article Pages

```xml
<!-- Article jcr:content -->
<jcr:content
    jcr:primaryType="cq:PageContent"
    jcr:title="Tech Innovation"
    cq:tags="[newspaper:news/tech]"
    sling:resourceType="newspaper/components/page"/>
```

### 3. Create Tag Taxonomy

```
http://localhost:4502/libs/cq/tagging/gui/content/tags.html

Create:
/content/cq:tags/newspaper/
├── news/
│   ├── tech/
│   ├── politics/
│   └── business/
├── opinion/
└── sport/
```

## ⚙️ Component Configuration

### Dialog Options

Authors có thể config trong component dialog:

1. **Properties Tab:**
   - `hideCurrent`: Hide current page from breadcrumb
   - `showHidden`: Show pages có `hideInNav=true`
   - `startLevel`: Navigation start level
   - `disableShadowing`: Disable shadowing

2. **Tag Configuration Tab:**
   - `selectedTag`: Override tag detection (nếu article có nhiều tags)

## 📈 Performance

- **Shallow depth**: Language root ở depth 3 (fast lookup)
- **Direct path resolution**: Không cần search/query
- **Skip articles folder**: Không traverse articles hierarchy
- **Efficient**: O(n) complexity where n = số segments trong tag

## ✅ Advantages

1. **Simple & Fast**: Path-based lookup, không cần query
2. **MSM Compatible**: Hoạt động với Live Copy structure
3. **Maintainable**: Dễ debug với clear logs
4. **Flexible**: Hỗ trợ multi-level tags
5. **i18n Ready**: Page titles tự động theo ngôn ngữ

## 🚀 Deployment Status

✅ **BUILD SUCCESS**  
✅ **Deployed:** http://localhost:4502/system/console/bundles  
✅ **Bundle:** newspaper.core-1.0.0-SNAPSHOT  
✅ **Version:** 1.1.0 (MSM Path-based)

## 📝 Testing

### Test Case 1: English Article
```
URL: http://localhost:4502/content/newspaper/language-masters/en/articles/2024/04/15/article.html
Expected: Home > News > Tech > Article Title
```

### Test Case 2: Vietnamese Article (MSM)
```
URL: http://localhost:4502/content/newspaper/language-masters/vi/articles/2024/04/15/bai-viet.html
Expected: Trang chủ > Tin tức > Công nghệ > Tiêu đề bài viết
```

### Test Case 3: Missing Topic Page
```
Article tag: newspaper:news/business
Topic page: /content/.../en/news/business (không tồn tại)
Expected: Home > News > Article (skip business)
```

### Test Case 4: No Tags
```
Article không có cq:tags property
Expected: Home > Article Title
```

## 🔧 Troubleshooting

### Issue: Breadcrumb chỉ hiển thị "Home > Article"

**Check:**
1. ✅ Article có `cq:tags` property?
   - Navigate: `/crx/de` → article → `jcr:content` → check `cq:tags`
2. ✅ Topic pages tồn tại?
   - Check: `/content/.../en/news` và `/content/.../en/news/tech`
3. ✅ Tags đúng format?
   - Format: `newspaper:news/tech` (namespace + hierarchy)

**Solution:**
```bash
# Add tags to article
cq:tags = ["newspaper:news/tech"]

# Verify topic pages exist
/content/.../en/news → Exists? Check in CRXDE
/content/.../en/news/tech → Exists? Check in CRXDE
```

### Issue: Log "Topic page not found"

```
WARN [BreadcrumbModel] Topic/subtopic page not found at path: .../en/news
```

**Solution:** Create topic page at expected path:
```
/content/newspaper/language-masters/en/news
```

## 📄 Related Documents

- Initial implementation: `BREADCRUMB_IMPLEMENTATION_SUMMARY.md`
- Path fix: `BREADCRUMB_PATH_FIX.md`
- Tag-based (deprecated): `BREADCRUMB_TAG_BASED_DISCOVERY.md`

---

**Date:** April 15, 2026  
**Version:** 1.1.0 (MSM Path-based - Final)  
**Status:** ✅ Production Ready  
**Approach:** Simple path-based resolution for MSM structure
