## 技术栈
JDK 版本为21.0.18
SpringBoot 版本为4.0.6
SpringCloud 版本为2025.1.1

使用pom.xml和maven管理依赖，从maven中央仓库获取依赖

持久层采用mysql + mybatis,禁止使用mybatis-plus

面向玩家的读接口需要有缓存层设计，采用redis，缓存过期时间统一为10分钟，要有防缓存穿透的机制

接口风格不要死板的套用restful，读接口用get方法，写接口用post方法,接口参数采用json格式
接口返回值采用json格式

对于json来说，要使用google的gson而非其他json解析库
创建几个统一的拦截器，所有java后端服务采用相同的实现
    - 登录拦截器
        - 如果某接口有特殊注解，则跳过登录校验，否则校验登录态
    - 权限拦截器
    - 数据校验拦截器
    - 日志拦截器
        - 记录请求参数和返回值，如果涉及文件上传的接口，则不要输出文件串的内容
        - 记录接口调用时间，单位毫秒
        - 记录接口调用时的参数和response的body。如果是get请求，则参数用key=value格式，如果是post请求，则参数用body的raw内容
        - 被调用日志放在logs/api.log文件中
    - 异常拦截器
        - 捕获所有异常，返回明确的错误信息，需要进行一定的包装


日志方面，统一使用log4j2，在logs目录下生成日志文件，info.log、error.log、api.log
所有日志文件采用小时滚动策略，每个小时生成一个日志文件，文件名格式为`yyyy-MM-dd_HH.log`。仅保留最近24小时的日志文件。

## 数据库 schema 约定
所有后端服务共用 `skada` 数据库。schema.sql 统一放在管理平面后端项目中（mng-service），内含全部表的建表语句，其他后端项目不再包含 schema.sql。