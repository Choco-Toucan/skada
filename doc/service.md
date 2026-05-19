## 技术栈
JDK 版本为21.0.18
SpringBoot 版本为4.0.6
SpringCloud 版本为2025.1.1

使用pom.xml和maven管理依赖，从maven中央仓库获取依赖

持久层采用mysql + mybatis,禁止使用mybatis-plus
mybatis版本用4.0.1

## 缓存设计
面向玩家的读接口需要有缓存层设计，采用redis，缓存过期时间统一为10分钟，要有防缓存穿透的机制

## 接口设计
接口风格不要死板的套用restful，读接口用get方法，写接口用post方法,接口参数采用json格式
接口返回值采用json格式

对于json来说，要使用google的gson而非其他json解析库

除了服务端不可能的框架级系统级问题以外，http code始终响应200，其它错误用body中的业务层code标识。
对于接口的业务层错误码来说，分成三个层级
    - 10000-19999：系统错误码，用于表示系统内部的错误或者必传参数缺失、请求格式错误等静态错误
    - 20000-29999：接入层错误码，用于表示登录态失效、权限不足、租户状态异常等错误
    - 30000-39999：业务错误码，用于表示业务逻辑上的错误

### OpenAPI 约定 (即api-service部分)
对于saas的open api，有以下约定
SAAS的基础参数，比如租户ID，安全sercret等，统一放在header中传递，body或者query parameter中仅包含接口本身的业务参数，不包含基础参数。
    - X-Tenant-Id：租户ID
    - X-Sign：sha256({timestamp} + {secretKey} + skada)
    - X-Timestamp：当前时间戳，单位毫秒，与服务器时间的偏移量超过5分钟的请求将被拒绝

### 管理后台API 约定 (即mng-service部分)
使用JWT token 模式进行校验

#### 拦截器设计
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


## 日志策略
日志方面，统一使用log4j2，在logs目录下生成日志文件，info.log、error.log、api.log
所有日志文件采用小时滚动策略，每个小时生成一个日志文件，文件名格式为`yyyy-MM-dd_HH.log`。仅保留最近24小时的日志文件。

## 数据库 schema 约定
所有后端服务共用 `skada` 数据库。schema.sql 统一放在管理平面后端项目中（mng-service），内含全部表的建表语句，其他后端项目不再包含 schema.sql。