package com.example.webview.new_webview
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import com.example.webview.R
import kotlinx.android.synthetic.main.activity_example.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class DwonloadsActivity : AppCompatActivity() {

    companion object {
        val INPUT_FILE_REQUEST_CODE = 13
    }


    private var webChromeClient: VideoEnabledWebChromeClient? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
   // private var webUrl="https://support.winds.co.in/support/home"
    private var webUrl="https://m.youtube.com"
    private lateinit var mContext:Context


    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        mContext = this

        try {
            initView()
        } catch (e: Exception) {
        }

    }


    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetJavaScriptEnabled")
    @Throws(Exception::class)
    private fun initView() {

        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true

        try {
            if (webView!!.url == null) {
                webView!!.loadUrl(webUrl.trim())
            }
        } catch (e: Exception) {
        }

        initWebViewLoad()
        progressBar!!.visibility = View.VISIBLE


        val loadingView = layoutInflater.inflate(R.layout.view_loading_video, null) // Your own view, read class comments
        val webChromeClient = object : VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, loadingView, webView!!) // See all available constructors...
        {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                try {
                    if (newProgress == 100) {
                        progressBar!!.visibility = View.GONE
                    }
                } catch (ignored: Exception) {
                }
            }

            override fun onShowFileChooser(
                webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback

                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent!!.resolveActivity(packageManager) != null) {
                    // Create the File where the photo should go
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.e("TAGS", "Unable to create Image File", ex)
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile)
                        )
                    } else {
                        takePictureIntent = null
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"

                val intentArray: Array<Intent?>
                if (takePictureIntent != null) {
                    intentArray = arrayOf(takePictureIntent)
                } else {
                    intentArray = arrayOfNulls(0)
                }

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
                return true
            }
        }

        webView!!.webChromeClient = webChromeClient


    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initWebViewLoad() {
        webView!!.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalFilesDir(this@DwonloadsActivity, Environment.DIRECTORY_DOWNLOADS, ".pdf")
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(applicationContext,"Your respective selected  brochures have been downloaded successfully", Toast.LENGTH_LONG).show()
        }
        webView!!.setOnClickListener { webView!!.loadUrl(webUrl) }
        webView!!.webViewClient = object : WebViewClient() {
            @SuppressLint("IntentReset")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("tel:")) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                    startActivity(intent)
                    return true
                }
                if (url.startsWith("mailto:")) {
                    val intent = Intent(Intent.ACTION_SENDTO) // it's not ACTION_SEND
                    intent.type = "text/plain"
                    intent.data = Uri.parse(url)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    return true
                }


                return false
            }
        }

    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        return File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        var results: Array<Uri>? = null

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }

        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
        return
    }


    override fun onBackPressed() {
        if (!webChromeClient!!.onBackPressed()) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                super.onBackPressed()
            }
        }
    }
}
