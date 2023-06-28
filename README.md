# 红云 Arthas 加载器

## 使用方法

下载此插件，将插件移动到 ./plugins 目录，将会在启动时运行 arthas

在加载完成后可通过浏览器打开 http://localhost:8563 访问 arthas web ui

## 配置文件

./plugins/hc-arthas/config.yml

```yaml
# 红云 Arthas bukkit 集成配置文件
# https://arthas.aliyun.com/doc/arthas-properties.html

# 监听的 IP 地址
ip: 127.0.0.1
# 监听的 telnet 端口
telnetPort: 3658
# 监听的 http 端口
httpPort: 8563

# 连接超时时间（单位：秒）
sessionTimeout: 1800

# 登陆用户名
# username: hc-arthas
# 登陆密码
# password: hc-arthas

# 本地的连接是否跳过登陆
localConnectionNonAuth: true

# 中转服务器地址
# tunnelServer: ws://127.0.0.1:7777/ws

# 禁止指定命令
disabledCommands:
  - stop

#### 高级设置，不理解请勿改动 ####
enhanceLoaders:
  - java.lang.ClassLoader
  - org.bukkit.plugin.java.PluginClassLoader
```

## 用例
### 不知道日志是哪个插件打印的
```shell
watch -x 2 org.apache.logging.log4j.core.config.LoggerConfig processLogEvent '{params[0].message.message, @java.lang.Thread@currentThread().getStackTrace()}' 'params[0].message.message.contains("日志中的一部分内容")'
```

### 不知道类是哪个插件添加的
```shell
sc -d com.google.gson.Gson
```

期待您提供更多用例

## License
本插件所用所有代码均为原创,不存在借用/抄袭等行为

- JvmHacker by InkerBot following MIT License
- Arthas by Alibaba following Apache 2.0 License