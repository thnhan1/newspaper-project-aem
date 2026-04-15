1) OSGi Service + QueryBuilder + Cache (default 1h)
Service: com.fa.core.services.ArticleQueryService
Impl: com.fa.core.services.impl.ArticleQueryServiceImpl
OSGi config (metatype):
Cache TTL: cache_ttl_seconds (default 3600s = 1h)
Cache max entries: cache_max_entries (default 200)
Query theo 2 mode:
TAGS: QueryBuilder filter cq:Page với jcr:content/cq:tags (OR nhiều tag)
FOLDER: traverse folder (giữ như option thay thế)
2) Article List Sling Model chỉ gọi service
ArticleListModel giờ chỉ:
đọc dialog props
build QueryParams
gọi articleQueryService.getArticles(request.getResourceResolver(), params)
map sang list để HTL render
Không còn viết query/traversal trong model nữa.
3) Dialog mới cho Article List
Trong Article List dialog có thêm:

Query mode: tags / folder
Categories (cq:tags): tagfield multiple
Folder root: dùng khi mode=folder
Featured article: chọn path bài nổi bật
Exclude featured: mặc định true (featured không bị lặp trong list)
🔧 Bạn cấu hình thế nào (use-case của bạn)
Articles nằm trong /content/.../articles/yyyy/mm/dd/...
Phân loại bằng cq:tags → chọn:
Query mode = By cq:tags
Categories: chọn các tag như newspaper:news/tech…
List root: chọn root ngôn ngữ (vd /content/newspaper/vn/vi) hoặc ngay /content/newspaper/vn/vi/articles
Featured article: pick 1 bài bất kỳ
Nếu bạn muốn featured “luôn nằm đầu + không phụ thuộc sort”, hiện tại service đã làm đúng như vậy.