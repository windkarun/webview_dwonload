package com.example.webview.new_webview
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.*
import android.widget.Toast
import com.example.webview.R
import kotlinx.android.synthetic.main.activity_example.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ExampleActivity : AppCompatActivity() {

    companion object {
        val INPUT_FILE_REQUEST_CODE = 13
    }

    // refer this link  https://github.com/cprcrack/VideoEnabledWebView

    private var webChromeClient: VideoEnabledWebChromeClient? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    internal var webUrl: String? = "http://m.youtube.com"
    internal var pdfUrl = ""
    private val STORAGE_PERMISSION = 100
    private lateinit var mContext:Context


    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        mContext=this

        progressBar!!.visibility = View.VISIBLE
        initView()
        val loadingView = layoutInflater.inflate(R.layout.view_loading_video, null) // Your own view, read class comments
        webChromeClient = object : VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, loadingView, webView!!) // See all available constructors...
        {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                try {
                    if (newProgress == 100) {
                        progressBar!!.visibility = View.GONE
                    }
                } catch (ignored: Exception) {
                }
            }
        }

        webView!!.webChromeClient = webChromeClient as VideoEnabledWebChromeClient
        webView!!.loadUrl(webUrl!!)


    }



    @SuppressLint("SetJavaScriptEnabled")
    private fun initView() {
        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true

        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                    webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: WebChromeClient.FileChooserParams): Boolean {

                try {
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
                        }

                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            mCameraPhotoPath = "file:" + photoFile.absolutePath
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile))
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

                } catch (ignored: Exception) {
                }

                return true
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                try {
                    if (newProgress == 100) {
                        progressBar!!.visibility = View.GONE
                    }
                } catch (ignored: Exception) {
                }

            }


        }

        webView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url.contains("mailto:")) {
                    startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
                    return true
                } else if (url.startsWith("tel:")) {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))
                    return true
                } else if (url.contains("downloads")) {
                    // Util.openUrlInBrowser(url, WebViewActivity.this);
                    return true
                } else if (url.contains("www.winds.co.in")) {
                    view.loadUrl(url)
                    progressBar!!.visibility = View.VISIBLE

                } else {
                    view.loadUrl(url)
                    progressBar!!.visibility = View.VISIBLE
                }
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                when {
                    url.contains("mailto:") -> {
                        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
                        return true
                    }
                    url.startsWith("tel:") -> {
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))
                        return true
                    }
                    url.contains("downloads") -> //   Util.openUrlInBrowser(url, WebViewActivity.this);
                        return true
                    url.contains("www.winds.co.in") -> {
                        view.loadUrl(url)
                        progressBar!!.visibility = View.VISIBLE
                    }
                    else -> {
                        view.loadUrl(url)
                        progressBar!!.visibility = View.VISIBLE
                    }
                }
                return false
            }
        }
        if (webView!!.url == null) {
            if (webUrl != null) {
                webView!!.loadUrl(webUrl!!)
            }
        }

        proceedUrl(webView!!,webUrl!!)
        webView!!.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            pdfUrl = url
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                downloadPdf(url)
            } else {
                ActivityCompat.requestPermissions(this@ExampleActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)

            }
        }


    }


    private fun downloadPdf(url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) //Notify client once download is completed!
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "WindsPdf.pdf")
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(applicationContext, "Downloading File", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            print(e.printStackTrace())
        }

    }

    private fun proceedUrl(view: WebView, url: String) {
        when {
            url.contains("mailto:") -> startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
            url.startsWith("tel:") -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))
            else -> {
                view.loadUrl(url)
                progressBar!!.visibility = View.VISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION -> for (permission in permissions) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    // showToast("Please provide storage permission to download pdf");
                } else {
                    if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                        //allowed
                        downloadPdf(pdfUrl)
                    } else {
                        //set to never ask again
                        // showToast("Please provide storage permission to download pdf");
                    }
                }
            }
        }

    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val imageFile: File
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES)
        imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

        return imageFile
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        try {
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
        } catch (e: Exception) {
        }

        return
    }



    override fun onBackPressed() {
        if (!webChromeClient!!.onBackPressed()) {
            if (webView!!.canGoBack()) {
                webView!!.goBack()
            } else {
                super.onBackPressed()
            }
        }
    }

}
