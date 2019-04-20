package com.example.webview.new_webview

import android.media.MediaPlayer
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout

open class VideoEnabledWebChromeClient(activityNonVideoView: View, activityVideoView: ViewGroup, loadingView: View, webView: VideoEnabledWebView) : WebChromeClient(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {


    private var activityNonVideoView: View? = activityNonVideoView
    private var activityVideoView: ViewGroup? = activityVideoView
    private var loadingView: View? = loadingView
    private var webView: VideoEnabledWebView? = webView
    var isVideoFullscreen: Boolean = false
        private set
    private var videoViewContainer: FrameLayout? = null
    private var videoViewCallback: WebChromeClient.CustomViewCallback? = null
    private var toggledFullscreenCallback: ToggledFullscreenCallback? = null


    init {
        this.isVideoFullscreen = false
    }

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (view is FrameLayout) {
            val focusedChild = view.focusedChild
            this.isVideoFullscreen = true
            this.videoViewContainer = view
            this.videoViewCallback = callback

            // Hide the non-video view, add the video view, and show it
            activityNonVideoView!!.visibility = View.INVISIBLE
            activityVideoView!!.addView(videoViewContainer, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            activityVideoView!!.visibility = View.VISIBLE

            if (focusedChild is android.widget.VideoView) {
                focusedChild.setOnPreparedListener(this)
                focusedChild.setOnCompletionListener(this)
                focusedChild.setOnErrorListener(this)
            } else {
                if (webView != null && webView!!.settings.javaScriptEnabled && focusedChild is SurfaceView) {
                    var js = "javascript:"
                    js += "var _ytrp_html5_video_last;"
                    js += "var _ytrp_html5_video = document.getElementsByTagName('video')[0];"
                    js += "if (_ytrp_html5_video != undefined && _ytrp_html5_video != _ytrp_html5_video_last) {"
                    run {
                        js += "_ytrp_html5_video_last = _ytrp_html5_video;"
                        js += "function _ytrp_html5_video_ended() {"
                        run {
                            js += "_VideoEnabledWebView.notifyVideoEnd();" // Must match Javascript interface name and method of VideoEnableWebView
                        }
                        js += "}"
                        js += "_ytrp_html5_video.addEventListener('ended', _ytrp_html5_video_ended);"
                    }
                    js += "}"
                    webView!!.loadUrl(js)
                }
            }

            // Notify full-screen change
            if (toggledFullscreenCallback != null) {
                toggledFullscreenCallback!!.toggledFullscreen(true)
            }
        }
    }

    override fun onShowCustomView(view: View, requestedOrientation: Int, callback: WebChromeClient.CustomViewCallback) // Available in API level 14+, deprecated in API level 18+
    {
        onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        if (isVideoFullscreen) {
            activityVideoView!!.visibility = View.INVISIBLE
            activityVideoView!!.removeView(videoViewContainer)
            activityNonVideoView!!.visibility = View.VISIBLE

            if (videoViewCallback != null && !videoViewCallback!!.javaClass.getName().contains(".chromium.")) {
                videoViewCallback!!.onCustomViewHidden()
            }

            isVideoFullscreen = false
            videoViewContainer = null
            videoViewCallback = null

            if (toggledFullscreenCallback != null) {
                toggledFullscreenCallback!!.toggledFullscreen(false)
            }
        }
    }

    override fun getVideoLoadingProgressView()
            : View? {
        if (loadingView != null) {
            loadingView!!.visibility = View.VISIBLE
            return loadingView
        } else {
            return super.getVideoLoadingProgressView()
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        if (loadingView != null) {
            loadingView!!.visibility = View.GONE
        }
    }

    override fun onCompletion(mp: MediaPlayer) {
        onHideCustomView()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int)
            : Boolean {
        return false
    }

    fun onBackPressed(): Boolean {
        return if (isVideoFullscreen) {
            onHideCustomView()
            true
        } else {
            false
        }
    }

    interface ToggledFullscreenCallback {
        fun toggledFullscreen(fullscreen: Boolean)
    }

}
