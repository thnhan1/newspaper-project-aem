## Cấu trúc filesystem (Vault map cq:tags → _cq_tags):

```
ui.content/.../jcr_root/content/_cq_tags/newspaper/
├── .content.xml              ← namespace "newspaper"
├── sections/
│   └── .content.xml          ← 5 tags: news, opinion, sport, culture, lifestyle
└── subsections/
    └── .content.xml          ← 21 tags: top-stories, world, politics, ...
```

Mỗi tag có đủ 3 ngôn ngữ tương ứng với MSM setup:

<news
    jcr:primaryType="cq:Tag"
    jcr:title="News"
    jcr:title.de="Nachrichten"
    jcr:title.vi="Tin tức"/>
    
filter.xml đã thêm /content/cq:tags/newspaper với mode="merge" để không xóa tag của namespace khác khi deploy.

Tag ID sau khi deploy sẽ có dạng: newspaper:sections/news, newspaper:subsections/top-stories — dùng để gắn vào article pages ở bước sau.