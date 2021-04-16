# Botkit
Botkit is a program, and a framework that helps to create modular [Telegram Bots](https://core.telegram.org/bots).

# Gradle
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.AgeOfWar:Botkit:0.1'
}
```

# Usage
```text
Usage: java -jar ${programFile.name} [options]
where options include:
    -token=<token> overrides token in bot.json
    -dir=<working directory> changes the working directory
    -? -h -help prints this help message to the output stream
```

#Example
![example](example.png)

# Plugin Example
This plugin re-sends messages

```kotlin
package com.example.myplugin

import com.github.ageofwar.botkit.plugin.*
import com.github.ageofwar.ktelegram.*

class MyPlugin : Plugin() {
    override suspend fun init() {
        registerUpdateHandlers(RepeatCommand(api)/*, ...*/)
    }
}

class RepeatCommand(private val api: TelegramApi) : UpdateHandler {
    override suspend fun handle(update: Update) = update.handleMessage { message ->
        val content = message.toMessageContent()
        if (content != null) {
            api.sendMessage(message.messageId, content)
        }
    }
}
```

Remember to add `botkit.properties` file in your resource source set:
```properties
name=MyPlugin
pluginClassName=com.example.myplugin.MyPlugin
```