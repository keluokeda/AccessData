# AccessData Android13及以下访问Data目录

最近在写一个安卓[炉石记牌器](https://github.com/keluokeda/hs_tracker)，需要访问炉石app外部存储目录来写入配置文件和读取log日志，也是就是需要读取某个制定app的外部存储目录。下面是具体步骤：
### 1，不需要再清单文件里面申请权限

### 2，创建请求访问Data目录权限合约
```kotlin
class RequestAccessAppDataDir : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {


        val dirUri = createAppDataDirUri(input)

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        val documentFile = DocumentFile.fromTreeUri(context.applicationContext, dirUri)!!
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentFile.uri)

        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data
    }

}
```

### 3，创建启动器

```kotlin
 ///请求访问目标app的data目录
        val requestAccessDataDirLauncher = registerForActivityResult(RequestAccessAppDataDir()) {
            if (it != null) {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
```
启动回调里面的代码意思是：如果成功，就请求持久访问该目录的权限，否则下次就不能访问了

### 4，启动启动器
```kotlin
            requestAccessDataDirLauncher.launch(targetPackageName ?: "")

```
targetPackageName的意思是目标app的包名

### 5，同意授权
![授权页面](https://upload-images.jianshu.io/upload_images/3690197-857aedf002eb2b3a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这个页面在不同系统版本会有差异

### 6，结束
当用户同意授权返回后，就能对目标app的data目录进行操作了。

[代码地址](https://github.com/keluokeda/AccessData)
