<h1 align="center">Minio for solon</h1>

<div align="center">
Author noear，iYarnFog
</div>

## ✨ 特性

- 🌈 无厂商捆绑，免除后顾之忧
- 📦 开箱即用的高质量组件。

## 📦 安装

```xml
<dependency>
    <groupId>org.noear</groupId>
    <artifactId>minio-solon-cloud-plugin</artifactId>
</dependency>
```

## ⚙️ 配置

```yaml
solon.cloud.minio:
  accessKey: 'Q3AM3UQ***'
  secretKey: 'zuf+tfteSlswRu7BJ86w***'
  file:
    enable: true                  #是否启用（默认：启用）
    endpoint: 'https://play.min.io'
    regionId: 'us-west-1'
    bucket: 'asiatrip'
```

## 🔨 示例

```java
//常规使用
public class DemoApp {
    public void main(String[] args) {
        SolonApp app = Solon.start(DemoApp.class, args);

        String key = "test/" + Utils.guid();
        String val = "Hello world!";

        //上传媒体
        Result rst = CloudClient.file().put(key, new Media(val));

        //获取媒体，并转为字符串
        String val2 = CloudClient.file().get(key).bodyAsString();
    }
}

//这样，可以获取其原始接口
MinioClient client = ((CloudFileServiceMinioImpl)CloudClient.file()).getClient();
```

```java
    // 文件上传接口
    @Post
    @Mapping("/file/upload")
    public Object upload(UploadedFile file) throws Exception {
        try{
            InputStream inputStream = file.getContent();
            String contentType = file.getContentType();
            long fileSize = file.getContentSize();
            String fileName = file.getName();
            //上传文件
            return CloudClient.file().put(fileName, new Media(inputStream, contentType, fileSize));
        } finally {
            file.delete();
        }
   }

```
