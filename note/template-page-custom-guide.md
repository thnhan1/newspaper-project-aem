# Hướng dẫn Thêm Tab và Field mới vào Giao diện Page Properties trong AEM

Tài liệu này hướng dẫn từng bước cách tùy chỉnh hoặc thêm mới một Tab và các trường (fields) nhập liệu riêng biệt vào cửa sổ Page Properties của một Page Template cụ thể trong Adobe Experience Manager (AEM).

## Tổng quan quy trình
Để thêm vào giao diện Page Properties, chúng ta không can thiệp trực tiếp vào Template mà phải tuân theo luồng sau:
1. Tạo một **Page Component** (trong `ui.apps`) kết thừa (inherit) từ Page component chung.
2. Ghi đè (override) hoặc mở rộng `cq:dialog` trong Page Component đó.
3. Cấu hình **Page Template** (trong `conf/...`) để trỏ đến Page Component vừa tạo.

---

## Các bước thực hiện

### Bước 1: Tạo Page Component tùy chỉnh (Custom Page Component)
Thông thường, các trang trên hệ thống sử dụng một component trang cơ bản (ví dụ: `core/wcm/components/page/v3/page`). Để thiết kế cấu hình riêng cho một loại trang cụ thể (ví dụ: *Article Page*), bạn tạo một component mới kế thừa component gốc.

**Vị trí:** `ui.apps/src/main/content/jcr_root/apps/newspaper/components/content/articlepage`

Tạo file thủ công cấu hình cho component (file `.content.xml` định nghĩa Component):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:description="Article Page Component"
          jcr:primaryType="cq:Component"
          jcr:title="Article Page"
          sling:resourceSuperType="core/wcm/components/page/v3/page"
          componentGroup="Newspaper - Page"/>
```
> **Lưu ý:** Thuộc tính `sling:resourceSuperType` vô cùng quan trọng, nó giúp AEM kế thừa lại toàn bộ giao diện properties gốc của AEM, để ta chỉ phải thêm tab thay vì viết lại từ đầu.

### Bước 2: Khai báo `cq:dialog` để thêm Tab
Bạn tạo tiếp một thư mục tên là `_cq_dialog` nằm trong component vừa tạo `/articlepage/_cq_dialog`. Bên trong đó tạo file `.content.xml`.

AEM sử dụng kiến trúc **Sling Resource Merger** (ẩn sau cấu trúc `granite/ui`). Khi bạn khai báo file `cq:dialog` và đặt các items có cấu trúc giống đường dẫn dialog gốc, AEM sẽ hợp nhất (merge) các field này vào hộp thoại gốc.

**File:** `ui.apps/src/main/content/jcr_root/apps/newspaper/components/content/articlepage/_cq_dialog/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    xmlns:jcr="http://www.jcp.org/jcr/1.0"
    xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    xmlns:cq="http://www.day.com/jcr/cq/1.0"
    xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
    jcr:primaryType="nt:unstructured"
    jcr:title="Article Page"
    sling:resourceType="cq/gui/components/authoring/dialog"
    sling:resourceSuperType="core/wcm/components/page/v3/page/cq:dialog"> <!-- Kế thừa Dialog Page V3 -->
    
    <content jcr:primaryType="nt:unstructured">
        <items jcr:primaryType="nt:unstructured">
            <!-- Tìm đến đúng vị trí "tabs" trong cấu trúc gốc của Page -->
            <tabs jcr:primaryType="nt:unstructured">
                <items jcr:primaryType="nt:unstructured">
                    
                    <!-- Khai báo Tab tùy chỉnh (VD: Content Fragment Tab) -->
                    <contentFragmentTab
                        jcr:primaryType="nt:unstructured"
                        jcr:title="Content Fragment"
                        sling:orderBefore="pwa"  <!-- Xếp tab này trước tab có tên là 'pwa' (ẩn hoặc đổi order tùy ý) -->
                        sling:resourceType="granite/ui/components/coral/foundation/container"
                        margin="{Boolean}true">
                        <items jcr:primaryType="nt:unstructured">
                            <columns
                                jcr:primaryType="nt:unstructured"
                                sling:resourceType="granite/ui/components/coral/foundation/fixedcolumns"
                                margin="{Boolean}true">
                                <items jcr:primaryType="nt:unstructured">
                                    
                                    <!-- QUAN TRỌNG: Column bắt buộc phải là một container -->
                                    <column
                                        jcr:primaryType="nt:unstructured"
                                        sling:resourceType="granite/ui/components/coral/foundation/container">
                                        <items jcr:primaryType="nt:unstructured">
                                            
                                            <!-- Định nghĩa Field cụ thể của bạn (VD: Pathfield) -->
                                            <contentFragment
                                                jcr:primaryType="nt:unstructured"
                                                sling:resourceType="granite/ui/components/coral/foundation/form/pathfield"
                                                cq:showOnCreate="true"
                                                fieldLabel="Content Fragment"
                                                fieldDescription="Chọn bài viết Content Fragment"
                                                name="./contentFragment"
                                                rootPath="/content/dam" />
                                                
                                        </items>
                                    </column>
                                </items>
                            </columns>
                        </items>
                    </contentFragmentTab>

                </items>
            </tabs>
        </items>
    </content>
</jcr:root>
```

#### Những lỗi thường gặp ở Bước 2:
- Tab hiển thị ra nhưng bị "trống trơn": Nguyên nhân thường do thiếu `sling:resourceType="granite/ui/components/coral/foundation/container"` ở các node `<column>` hoặc `<contentFragmentTab>`. Khi dùng `fixedcolumns`, AEM bắt buộc child columns của nó phải là vùng chứa (container).

### Bước 3: Cấu hình Template (Template/Policy mapping)
Sau khi đã tạo Page Component có chứa Dialog bên `ui.apps`, bạn cần báo cho Template biết để sử dụng cái Component đó, thay vì component gốc.

Truy cập mã nguồn thư mục mẫu template của bạn (thường tại `ui.content`), trên template cần sử dụng:
**Vị trí:** `ui.content/src/main/content/jcr_root/conf/newspaper/settings/wcm/templates/article-page/structure/.content.xml`

Mở file và cập nhật/hiệu đính dòng `sling:resourceType`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" ...>
    <jcr:content
        jcr:primaryType="cq:PageContent"
        sling:resourceType="newspaper/components/content/articlepage"> 
        <!-- Đổi resourceType cấu trúc Template để kết nối đến Component vừa làm -->
        
        <root .../>
    </jcr:content>
</jcr:root>
```

---

## Build code và kiểm tra
1. Chạy lệnh build package qua maven ở root project của bạn:
   ```bash
   mvn clean install -PautoInstallPackage
   ```
   (Hoặc nếu dùng VLT/AEM Repo Sync thì sync folder `ui.apps` và `ui.content` lên hệ thống).

2. Truy cập vào AEM Author (Sites).
3. Mở tính năng Page Properties (`View properties`) hoặc bấm vào icon ⚙️ ở phía Edit trang của một **Article Page** được tạo bởi Template này.
4. Xem kết quả: Bạn sẽ thấy Tab **"Content Fragment"** xuất hiện và có chứa thanh bộ chọn đường dẫn. Dữ liệu khi save sẽ được lưu xuống dưới node `jcr:content` (`./contentFragment`) của page đó.
