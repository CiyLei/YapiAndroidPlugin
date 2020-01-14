# Yapi 数据生成插件

## 下载安装插件

1. 下载此[插件](https://github.com/CiyLei/YapiAndroidPlugin/blob/master/Plugin/Plugin.zip?raw=true)

2.  `File` -> `Settings...` -> `Plugins` -> `Installed` -> `Install Plugin from Disk...` 选择插件，然后重启 IDE
![安装插件](https://raw.githubusercontent.com/CiyLei/YapiAndroidPlugin/master/img/%E5%AE%89%E8%A3%85%E6%8F%92%E4%BB%B6.png)

## 生成模型

1. 在 yapi 平台中找到具体的项目，然后复制项目的 token

    ![yapi选择项目](https://raw.githubusercontent.com/CiyLei/YapiAndroidPlugin/master/img/yapi%E9%80%89%E6%8B%A9%E9%A1%B9%E7%9B%AE.png)
    
    ![复制项目token](https://raw.githubusercontent.com/CiyLei/YapiAndroidPlugin/master/img/%E5%A4%8D%E5%88%B6%E9%A1%B9%E7%9B%AEtoken.png)

2. 点击图标,输入 yapi 的地址和项目的 token ,点击 Next

    ![选择图标](https://raw.githubusercontent.com/CiyLei/YapiAndroidPlugin/master/img/%E9%80%89%E6%8B%A9%E5%9B%BE%E6%A0%87.png)
    
    ![输入token](https://raw.githubusercontent.com/CiyLei/YapiAndroidPlugin/master/img/%E8%BE%93%E5%85%A5token.png)

3. 确认好模块、生成模型的包路径、然后选择需要分析的 api ，然后点击 Next 开始生成

    ![选择分析的api](https://raw.githubusercontent.com/CiyLei/YapiAndroidPlugin/master/img/%E9%80%89%E6%8B%A9%E5%88%86%E6%9E%90%E7%9A%84api.png)

    > 为了性能和安全，不要勾选多余的 api ，这次迭代牵扯到了哪些接口，就勾选哪些接口

    > 如果勾选的太多，会在进度条 100% 卡一会，请不要着急

## 生成模型说明

此插件一共会生成3种类

* URLConstant.kt
* ApiService.kt
* 每个接口所需要的 Model

> 插件每次生成都会覆盖文件和内容的，所以不建议手动修改这里类（有例外）

1. URLConstant.kt

    存放了所有接口的 url 地址，值得注意的是里面有两个特殊的变量 `PREFIX` 和 `SUFFIX`，代表 url 的前缀和后缀，他们俩是不会被重新生成覆盖的，所以可以放心地进行自定义修改

2. ApiService.kt

    因为公司所有项目网络请求都是用 Retrofit + Rxjava 的，所以也就生成了一个 Retrofit 所需的接口请求接口类，每个接口方法的默认返回类型为 `Observable<BaseResponse<XXXModel>>`

    如果是 Post ，那个这个方法会有一个入参，请求的时候会将此参数装换为 json 数据发送的（Retrofit需要添加Json的装换器：如GsonConverterFactory）；构造入参的类型的时候，如果在构造方法里面的字段代表必填，否则是选填

3. 每个接口所需要的 Model

    这里的模型包括 Post 接口所需的入参数据模型和所有接口响应的数据模型

## 注意事项

记得在 retrofit 中指定 rxjava 和 json 的转换器

```java
.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
.addConverterFactory(GsonConverterFactory.create())
```

## 可能会遇到的问题

1. 为什么在 ApiService 中有些接口的数据模型是 Any？

    请在 yapi 中查看此 url 中定义的类型，如果他没定义，插件自然生成不了；如果数据模型的类型不对，也无法正确生成

    我自认为只可能是后端生成 yapi 的时候出了问题

2. 如果我的项目中需要多个 yapi 项目支持怎么办？

    这时候我建议在分析 api 的时候，修改包名，为每个 yapi 项目生成一套数据模型，然后在项目中通过 retrofit create 多个 ApiService 实现