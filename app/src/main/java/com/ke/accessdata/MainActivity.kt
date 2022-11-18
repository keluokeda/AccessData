package com.ke.accessdata

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import com.ke.accessdata.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ///请求访问目标app的data目录
        val requestAccessDataDirLauncher = registerForActivityResult(RequestAccessAppDataDir()) {
            if (it != null) {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            }

        binding.requestStoragePermission.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        binding.requestPermission.setOnClickListener {
            requestAccessDataDirLauncher.launch(targetPackageName ?: "")
        }

        binding.checkPermission.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("是否可以访问data目录 = ${canReadDataDir(targetPackageName ?: "")}")
                .show()
        }

        binding.createFile.setOnClickListener {

            val filesDocumentFile = getTargetFile(null)

            val result = filesDocumentFile?.createFile("plain/text", fileName)

            AlertDialog.Builder(this)
                .setMessage("创建文件结果 $result")
                .show()
        }

        binding.delete.setOnClickListener {


            val result = getTargetFile(fileName)?.delete()

            AlertDialog.Builder(this)
                .setMessage("删除文件结果 $result")
                .show()
        }

        ///写入内容到文件
        binding.write.setOnClickListener {

            val target = getTargetFile(fileName) ?: return@setOnClickListener

            contentResolver.openOutputStream(target.uri)
                ?.apply {
                    write(content.toByteArray())
                    close()
                }
        }

        binding.read.setOnClickListener {
            val targetFile = getTargetFile(fileName) ?: return@setOnClickListener

            contentResolver.openInputStream(targetFile.uri)
                ?.apply {
                    binding.fileContent.text = reader().readText()
                    close()
                }
        }
    }

    private fun getTargetFile(fileName: String?): DocumentFile? {
        val uri =
            createAppDataDirUri(targetPackageName!!)
        val documentFile =
            DocumentFile.fromTreeUri(applicationContext, uri) ?: return null

        val filesDocumentFile = documentFile.findFile("files") ?: return null

        return if (fileName == null) filesDocumentFile else filesDocumentFile.findFile(fileName)
    }

    /**
     * 文件名
     */
    private val fileName: String
        get() = binding.fileName.text!!.toString()

    /**
     * 目标包名
     */
    private val targetPackageName: String?
        get() = binding.targetPackageName.text?.toString()

    /**
     * 要写入的文件内容
     */
    private val content: String
        get() = binding.content.text!!.toString()
}

/**
 * 创建目标目录dir
 */
private fun createAppDataDirUri(packageName: String) =
    Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata%2F${packageName}")!!

/**
 * 是否可以访问data目录
 */
fun Context.canReadDataDir(packageName: String): Boolean {
    return DocumentFile.fromTreeUri(applicationContext, createAppDataDirUri(packageName))?.canRead()
        ?: false
}

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