# Breadcrumb Tag-Based Page Discovery - Implementation Guide

## 🎯 Overview

Breadcrumb component hiện tại đã được nâng cấp để **tìm topic pages dựa trên cq:tags** thay vì dựa vào path cứng. Điều này cho phép breadcrumb hoạt động với các URL đã được dịch trong môi trường đa ngôn ngữ.

## ✨ Tính Năng Mới

### Trước Đây (Path-based)
```
Article có tag: newspaper:news/tech
Code tìm kiếm tại: /content/.../en/news (path cứng)
❌ Không hoạt động với URL đã dịch: /content/.../vi/tin-tuc
```

### Bây Giờ (Tag-based)
```
Article có tag: newspaper:news/tech
Code tìm pages có cq:tags = "newspaper:news" và "newspaper:news/tech"
✅ Hoạt động với mọi URL: 
   - /content/.../en/news (English)
   - /content/.../vi/tin-tuc (Vietnamese)
   - /content/.../vn/vi/tin-tuc (Vietnam region)
```

## 🏗️ Cấu Trúc Content Hỗ Trợ

### Ví Dụ Multi-lingual với Live Copy

```
/content/newspaper/
├── language-masters/
│   ├── en/
│   │   ├── news/                    (cq:tags = ["newspaper:news"])
│   │   │   └── tech/                (cq:tags = ["newspaper:news/tech"])
│   │   └── articles/
│   │       └── 2024/04/15/
│   │           └── article-1        (cq:tags = ["newspaper:news/tech"])
│   │
│   └── vi/                          (Live Copy từ en/)
│       ├── tin-tuc/                 (cq:tags = ["newspaper:news"]) ← URL đã dịch
│       │   └── cong-nghe/           (cq:tags = ["newspaper:news/tech"])
│       └── articles/
│           └── 2024/04/15/
│               └── bai-viet-1       (cq:tags = ["newspaper:news/tech"])
│
└── vn/
    └── vi/                          (Live Copy hoặc cấu trúc riêng)
        ├── tin-tuc/                 (cq:tags = ["newspaper:news"])
        │   └── cong-nghe/           (cq:tags = ["newspaper:news/tech"])
        └── articles/...
```

## 🔧 Cách Hoạt Động

### 1. Extract Tag từ Article

```java
// Article page: /content/.../vi/articles/2024/04/15/bai-viet-1
// cq:tags = ["newspaper:news/tech"]

String tagPath = extractTagPath(); // Returns: "news/tech"
```

### 2. Build Tag Hierarchy

```java
String[] segments = tagPath.split("/"); // ["news", "tech"]

// Loop qua từng segment:
// Iteration 1: "newspaper:news"
// Iteration 2: "newspaper:news/tech"
```

### 3. Tìm Page theo Tag (Không phụ thuộc URL!)

```java
// Tìm page có tag "newspaper:news" dưới language root
Page topicPage = findPageByTag(languageRoot, "newspaper:news");

// Kết quả:
// - English: /content/.../en/news
// - Vietnamese: /content/.../vi/tin-tuc  ← URL khác nhưng vẫn tìm thấy!
```

### 4. Search Algorithm

```java
private Page findPageByTag(Page languageRoot, String tag) {
    // 1. Search trong direct children của language root
    Iterator<Page> children = languageRoot.listChildren(null, false);
    
    while (children.hasNext()) {
        Page child = children.next();
        
        // Skip articles folder
        if (child.getName().equals("articles")) {
            continue;
        }
        
        // Check nếu page này có tag mục tiêu
        if (pageHasTag(child, tag)) {
            return child; // Found!
        }
        
        // 2. Nếu là nested tag (news/tech), search đệ quy 2 cấp
        if (tag.contains("/")) {
            Page nestedPage = searchPageByTagRecursive(child, tag, 2);
            if (nestedPage != null) {
                return nestedPage;
            }
        }
    }
    
    return null; // Not found
}

private boolean pageHasTag(Page page, String targetTag) {
    Resource contentResource = page.getContentResource();
    ValueMap properties = contentResource.getValueMap();
    String[] tags = properties.get("cq:tags", String[].class);
    
    for (String tag : tags) {
        if (targetTag.equals(tag)) {
            return true;
        }
    }
    return false;
}
```

## 📋 Setup Requirements

### 1. Tag Configuration

Mỗi topic/subtopic page phải có `cq:tags` property:

```xml
<!-- /content/.../en/news/jcr:content -->
<jcr:content
    jcr:primaryType="cq:PageContent"
    jcr:title="News"
    cq:tags="[newspaper:news]"
    sling:resourceType="newspaper/components/page"/>

<!-- /content/.../vi/tin-tuc/jcr:content -->
<jcr:content
    jcr:primaryType="cq:PageContent"
    jcr:title="Tin tức"
    cq:tags="[newspaper:news]"          ← Same tag, different URL!
    sling:resourceType="newspaper/components/page"/>
```

### 2. Tag Taxonomy

Tags phải được tạo trong Tag Management:

```
/content/cq:tags/newspaper/
├── news/                    (newspaper:news)
│   ├── tech/                (newspaper:news/tech)
│   ├── politics/            (newspaper:news/politics)
│   └── business/            (newspaper:news/business)
├── opinion/                 (newspaper:opinion)
└── sport/                   (newspaper:sport)
```

**Tạo tags:** http://localhost:4502/libs/cq/tagging/gui/content/tags.html

### 3. Live Copy Setup (Optional)

Nếu dùng AEM Translation Framework:

1. **Create Language Copy:**
   - Source: `/content/newspaper/language-masters/en`
   - Target: `/content/newspaper/language-masters/vi`
   - Options: Include tags, Update references

2. **Translate URLs:**
   - English page: `news` → Vietnamese page: `tin-tuc`
   - Tags remain the same: `cq:tags = ["newspaper:news"]`

3. **Update references:**
   - AEM tự động cập nhật internal references
   - Breadcrumb tìm pages qua tags, không phụ thuộc URL

## 🎯 Use Cases

### Case 1: Standard Multi-lingual Site

```
English Article:
- Path: /content/.../en/articles/2024/04/15/tech-article
- Tag: newspaper:news/tech
- Breadcrumb: Home > News > Tech > Tech Article

Vietnamese Article (Live Copy):
- Path: /content/.../vi/articles/2024/04/15/bai-viet-cong-nghe
- Tag: newspaper:news/tech
- Topic Pages: /content/.../vi/tin-tuc/cong-nghe (có cq:tags)
- Breadcrumb: Home > Tin tức > Công nghệ > Bài viết công nghệ
```

### Case 2: Regional Variations

```
Vietnam Region:
- Path: /content/.../vn/vi/articles/2024/04/15/article
- Tag: newspaper:news/tech
- Topic Pages: /content/.../vn/vi/tin-tuc/cong-nghe
- Breadcrumb: Home > Tin tức > Công nghệ > Article
```

### Case 3: Missing Topic Page

```
Article có tag: newspaper:news/business
Topic page chưa tạo hoặc chưa có tag

Log:
WARN [BreadcrumbModel] Page with tag 'newspaper:news/business' not found under .../en

Result: Home > Article (skip missing topic)
```

## 📊 Performance Considerations

### Search Depth Limit

```java
// Tìm kiếm giới hạn 2 cấp sâu để tránh performance issues
private Page searchPageByTagRecursive(Page parent, String tag, int maxDepth) {
    if (maxDepth <= 0) {
        return null; // Stop recursion
    }
    // ... search logic
}
```

### Caching Strategy

```java
// Future enhancement: Cache tag → page mapping
private Map<String, String> tagToPageCache = new ConcurrentHashMap<>();

private Page findPageByTagCached(Page root, String tag) {
    String cacheKey = root.getPath() + ":" + tag;
    String cachedPath = tagToPageCache.get(cacheKey);
    
    if (cachedPath != null) {
        return pageManager.getPage(cachedPath);
    }
    
    Page found = findPageByTag(root, tag);
    if (found != null) {
        tagToPageCache.put(cacheKey, found.getPath());
    }
    
    return found;
}
```

## 🔍 Debugging

### Log Messages

```
INFO [BreadcrumbModel] Language root found: /content/newspaper/language-masters/vi
INFO [BreadcrumbModel] Building breadcrumb from tag path: news/tech
DEBUG [BreadcrumbModel] Looking for page with tag: newspaper:news
DEBUG [BreadcrumbModel] Searching for page with tag 'newspaper:news' under: .../vi
DEBUG [BreadcrumbModel] Found matching page: .../vi/tin-tuc with tag: newspaper:news
INFO [BreadcrumbModel] Found topic page by tag 'newspaper:news': .../vi/tin-tuc (title: Tin tức)
DEBUG [BreadcrumbModel] Looking for page with tag: newspaper:news/tech
DEBUG [BreadcrumbModel] Found matching nested page: .../vi/tin-tuc/cong-nghe with tag: newspaper:news/tech
INFO [BreadcrumbModel] Found topic page by tag 'newspaper:news/tech': .../vi/tin-tuc/cong-nghe
```

### Debug Checklist

1. ✅ **Check article tags:**
   ```
   http://localhost:4502/crx/de/index.jsp
   Navigate to: /content/.../articles/.../article/jcr:content
   Check property: cq:tags = ["newspaper:news/tech"]
   ```

2. ✅ **Check topic page tags:**
   ```
   Navigate to: /content/.../vi/tin-tuc/jcr:content
   Check property: cq:tags = ["newspaper:news"]
   ```

3. ✅ **Check logs:**
   ```
   http://localhost:4502/system/console/slinglog
   Filter: com.fa.core.models.BreadcrumbModel
   Level: DEBUG
   ```

## 🚀 Migration Guide

### From Path-based to Tag-based

#### Step 1: Add Tags to Existing Pages

```bash
# For English pages
/content/.../en/news → Add cq:tags = ["newspaper:news"]
/content/.../en/news/tech → Add cq:tags = ["newspaper:news/tech"]

# For Vietnamese pages (translated URLs)
/content/.../vi/tin-tuc → Add cq:tags = ["newspaper:news"]
/content/.../vi/tin-tuc/cong-nghe → Add cq:tags = ["newspaper:news/tech"]
```

#### Step 2: Create Tag Taxonomy

```bash
# Via AEM Tag Console
http://localhost:4502/libs/cq/tagging/gui/content/tags.html

Create structure:
newspaper/
├── news/
│   ├── tech/
│   └── business/
└── sport/
```

#### Step 3: Test Breadcrumb

```bash
# English article
http://localhost:4502/content/newspaper/language-masters/en/articles/.../article.html
Expected: Home > News > Tech > Article

# Vietnamese article (with translated URLs)
http://localhost:4502/content/newspaper/language-masters/vi/articles/.../bai-viet.html
Expected: Home > Tin tức > Công nghệ > Bài viết
```

## ⚙️ Configuration

### Dialog Options (Still Available)

Authors có thể override tag detection:

```xml
<!-- Component dialog -->
<selectedTag
    jcr:primaryType="nt:unstructured"
    sling:resourceType="cq/gui/components/coral/common/form/tagfield"
    fieldLabel="Primary Topic Tag"
    name="./selectedTag"
    rootPath="/content/cq:tags/newspaper"/>
```

**Use case:** Article có nhiều tags, author muốn chọn tag nào để build breadcrumb.

## 🎨 Best Practices

### 1. Consistent Tagging

```
✅ GOOD: All topic pages have same tag structure
   - EN: /en/news → newspaper:news
   - VI: /vi/tin-tuc → newspaper:news
   - Both found by breadcrumb!

❌ BAD: Inconsistent tags
   - EN: /en/news → newspaper:news
   - VI: /vi/tin-tuc → newspaper:tin-tuc (different tag)
   - Breadcrumb fails!
```

### 2. Tag Hierarchy

```
✅ GOOD: Clear parent-child relationship
   newspaper:news
   newspaper:news/tech
   newspaper:news/business

❌ BAD: Flat structure
   newspaper:news
   newspaper:tech (missing parent relationship)
```

### 3. Performance

```
✅ GOOD: Shallow structure (max 2-3 levels)
   newspaper:news/tech

❌ BAD: Deep nesting (slow search)
   newspaper:news/tech/ai/machine-learning/nlp
```

## 📈 Benefits

### 1. URL Flexibility
- Dịch URLs thoải mái: `news` → `tin-tuc`, `tech` → `cong-nghe`
- Breadcrumb vẫn hoạt động vì tìm theo tags

### 2. Content Reusability
- Same article có thể thuộc nhiều topics (multiple tags)
- Breadcrumb show đúng topic based on author selection

### 3. Maintenance
- Đổi cấu trúc URL không ảnh hưởng breadcrumb
- Chỉ cần giữ tags consistent

### 4. Multi-region Support
- Same tag structure cho tất cả regions
- Easy to scale: `en`, `vi`, `vn/vi`, `th/th`, etc.

## 🔮 Future Enhancements

### 1. Tag-based Query Caching
```java
@Reference
private TagManager tagManager;

// Cache tag → pages mapping in memory
private Map<String, Set<String>> tagIndex;
```

### 2. Query Builder Integration
```java
private Page findPageByTagWithQuery(String tag) {
    Map<String, String> predicates = new HashMap<>();
    predicates.put("path", languageRoot.getPath());
    predicates.put("type", "cq:Page");
    predicates.put("tagid", tag);
    predicates.put("p.limit", "1");
    
    Query query = queryBuilder.createQuery(
        PredicateGroup.create(predicates), 
        resourceResolver.adaptTo(Session.class)
    );
    
    SearchResult result = query.getResult();
    // Return first hit
}
```

### 3. Breadcrumb Cache
```java
@Reference
private CacheService cacheService;

private Collection<NavigationItem> getCachedBreadcrumb(String pageId) {
    String cacheKey = "breadcrumb:" + pageId;
    return cacheService.get(cacheKey, 
                            () -> buildBreadcrumbItems());
}
```

---

**Implementation Date:** April 15, 2026  
**Version:** 2.0.0 (Tag-based Discovery)  
**Status:** ✅ Deployed and Active  
**Bundle:** newspaper.core-1.0.0-SNAPSHOT
