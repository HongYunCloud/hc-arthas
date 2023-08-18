# 红云 Arthas 加载器

## RCE 警告

## 使用方法

下载此插件，将插件移动到 ./plugins 目录，将会在启动时运行 arthas

在加载完成后可通过浏览器打开 http://localhost:8563 访问 arthas web ui

如果你需要处理的异常在插件加载之前，你则需要使用 agent 方式来加载，即在启动参数 java 后添加 -javaagent:
plugins/hc-arthas-1.1-all.jar

```shell
# 添加前
java -Xmx 8G -Xms 8G -jar paper.jar
# 添加后
java -javaagent:plugins/hc-arthas-1.1-all.jar -Xmx 8G -Xms 8G -jar paper.jar
```

## 配置文件

位于 ./plugins/hc-arthas/config.yml

## 用例

### 不知道日志是哪个插件打印的

```shell
watch -x 2 org.apache.logging.log4j.core.config.LoggerConfig processLogEvent '{params[0].message.message,@java.lang.Thread@currentThread().getStackTrace()}' 'params[0].message.message.contains("日志中的一部分内容")'
```

### 不知道类是哪个插件添加的

```shell
sc -d com.google.gson.Gson
```

### 不知道哪个插件发送了聊天信息

Minecraft 1.12+

```shell
watch -x 2 net.minecraft.server.*.PlayerConnection sendPacket '{params[0],@java.lang.Thread@currentThread().getStackTrace()}' 'params.length>0 && params[0].class.name.endsWith("PacketPlayOutChat")'
```

Minecraft 1.17+

```shell
watch -x 2 net.minecraft.server.network.PlayerConnection * '{params[0],@java.lang.Thread@currentThread().getStackTrace()}' 'params.length==2 && (params[0].class.name == "net.minecraft.network.protocol.game.ClientboundPlayerChatPacket" || params[0].class.name == "net.minecraft.network.protocol.game.ClientboundSystemChatPacket")'
```

期待您提供更多用例

## License

本插件所用所有代码均为原创,不存在借用/抄袭等行为

- JvmHacker by InkerBot following MIT License
- Arthas by Alibaba following Apache 2.0 License