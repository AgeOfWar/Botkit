# Botkit
Botkit is a program, and a framework that helps to create modular [Telegram Bots](https://core.telegram.org/bots).

# Gradle
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.AgeOfWar:Botkit:0.6'
}
```

# Usage
```text
Usage: java -jar ${programFile.name} [options]
where options include:
    -token=<token> overrides token in bot.json
    -? -h -help prints this help message to the output stream
```

# Example
![example](example.png)

# Available commands
```text
disable <plugin|*>  - disables plugin or all plugins
enable <plugin|*>   - enables plugin or all plugins
reload [plugin|*]   - reloads plugin or all plugins
plugins             - shows enabled and available plugins
stop                - stops botkit
```
Plugins can add custom commands

# Plugin Example
This plugin re-sends messages

```kotlin
package com.example.myplugin

import com.github.ageofwar.botkit.plugin.*
import com.github.ageofwar.ktelegram.*

class MyPlugin : Plugin() {
    override suspend fun init() {
        registerUpdateHandlers(RepeatCommand(this))
    }
}

class RepeatCommand(private val plugin: Plugin) : UpdateHandler {
    override suspend fun handle(update: Update) = update.handleMessage { message ->
        val content = message.toMessageContent()
        if (content != null) {
            plugin.api.sendMessage(message.messageId, content)
        }
    }
}
```

Remember to add `botkit.properties` file in your resource source set:
```properties
name=MyPlugin
pluginClassName=com.example.myplugin.MyPlugin
apiVersion=0.1
```

## Some utility functions

| Function                                                                           | Description                                                                           |
|------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `Plugin.registerUpdateHandler(UpdateHandler)                                     ` | registers a handler for [Telegram updates](https://core.telegram.org/bots/api#update) |
| `Plugin.registerConsoleCommand(PluginCommand)                                    ` | registers a handler for console commands                                              |
| `Plugin.registerLogger(PluginLogger)                                             ` | registers a botkit logger                                                             |
| `Plugin.dispatchConsoleCommand(String)                                           ` | executes a console command                                                            |
| `Plugin.log(String?) / Plugin.warning(String?) / Plugin.error(String?,Throwable?)` | logging methods                                                                       |
| `Plugin.withPlugin<reified T : Plugin>(T.() -> R): R                             ` | uses external plugin                                                                  |
| `Plugin.readFile<R>(String, Charset, (Throwable) -> R, Reader.() -> R): R        ` | utility method for plugin data reading                                                |
| `Plugin.writeFile(String, Charset, (Throwable) -> R, Writer.() -> Unit)          ` | utility method for plugin data writing                                                |
| `Plugin.readFileAs(...) / Plugin.readFileOrCopy(...) / Plugin.writeFile(...)     ` | utility methods for json data reading/writing                                         |
| `String.template(vararg Pair<String, Any?>): String                              ` | formats a string, see [Freemarker](https://freemarker.apache.org/docs/index.html)     |

## commands.txt
```text
kick  - kicks an user
help  - shows an help message
hello - says hello
```
Adding this file into plugin data folder will update your bot commands when the plugin is enabled

## Other utility functions
`Plugin.registerCommandHandler(vararg commandName: String, ...)`

Registers a simple command that sends a list of messages saved in a file

```kotlin
registerCommandHandler("start", "help", fileName = "start.json" /* optional */)
```

### start.json
```json
[
  {
    "text": "*This* bot is very cool and uses [Botkit](https://github.com/AgeOfWar/Botkit)",
    "disable_web_page_preview": true
  },
  {
    "photo": "https://my.cool/image.png",
    "caption": "👍"
  }
]
```

------------

`Plugin.readFileOrCopy<reified T>(file: String, defaultPath: String, ...): T`

Reads a json file and if it is not present copies the file from resources (config/defaultPath)
