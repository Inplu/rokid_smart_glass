package com.rokid.glass.view;

import static com.rokid.glass.utils.ExtendUtilKt.dp2px;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;

import com.rokid.glesse.R;

public class ProgressWebView extends WebView {

    // 进度条
    // Progress indicator.
    private ProgressBar mProgressBar;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    public ProgressWebView(Context context) {
        this(context, null);
    }

    public ProgressWebView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initView(context);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initView(Context context) {
        // 创建进度条
        // Create the progress bar.
        mProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        // 设置加载进度条的高度
        // Set the loading progress bar height.
        // 进度条的高度，默认10px
        // Progress bar height; defaults to 10 px.
        int progressHeight = 4;
        mProgressBar.setLayoutParams(new LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp2px(progressHeight), 0, 0));

        // 进度条背景图
        // Progress bar background drawable.
        GradientDrawable background = new GradientDrawable();
        background.setColor(getResources().getColor(android.R.color.white));
        background.setCornerRadius(dp2px(1));

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(dp2px(1));
        gradientDrawable.setColor(getResources().getColor(R.color.green_normal_50));
        // 进度颜色图
        // Progress color drawable.
        ClipDrawable progress = new ClipDrawable(gradientDrawable, Gravity.START, ClipDrawable.HORIZONTAL);

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{background, progress});
        // 设置进度条图层
        // Set the progress bar drawable layers.
        mProgressBar.setProgressDrawable(layerDrawable);

        // 添加水平进度条到WebView
        // Add a horizontal progress bar to the WebView.
        addView(mProgressBar);

        WebSettings webSettings = getSettings();

        // 开启js脚本支持
        // Enable JavaScript support.
        webSettings.setJavaScriptEnabled(true);

        // 支持通过JS打开新窗口
        // Allow JavaScript to open new windows.
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // 只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
        // Use locally cached data whenever available, including expired or no-cache entries.
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        // 缓存模式如下：
        // Available cache modes:
        // LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
        // LOAD_CACHE_ONLY: read only local cached data without using the network.
        // LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
        // LOAD_DEFAULT: the default mode uses cache-control to decide whether to fetch data from the network.
        // LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
        // LOAD_NO_CACHE: fetch data only from the network without using the cache.
        // LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
        // LOAD_CACHE_ELSE_NETWORK uses locally cached data whenever available, including expired or no-cache entries.

        // 适配手机大小
        // Adapt to the phone screen size.
        // 设置自适应屏幕，两者合用
        // Use both settings together to fit the page to the screen.
        // 将图片调整到适合WebView的大小
        // Adjust images to fit the WebView.
        webSettings.setUseWideViewPort(true);
        // 缩放至屏幕的大小
        // Scale the page to the screen size.
        webSettings.setLoadWithOverviewMode(true);

        // 缩放操作
        // Zoom settings.
        // 支持缩放，默认为true。是下面那个的前提。
        // Enable zoom support, which defaults to true and is required for the following setting.
        webSettings.setSupportZoom(true);
        // 设置内置的缩放控件。若为false，则该WebView不可缩放
        // Enable the built-in zoom controls; the original note states that disabling them prevents zooming in this WebView.
        webSettings.setBuiltInZoomControls(true);
        // 隐藏原生的缩放控件
        // Hide the native zoom controls.
        webSettings.setDisplayZoomControls(false);

        // 其他细节操作
        // Other settings.
        // 设置可以访问文件
        // Allow file access.
        webSettings.setAllowFileAccess(true);

        // 支持自动加载图片
        // Enable automatic image loading.
        webSettings.setLoadsImagesAutomatically(true);

        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        setWebChromeClient(new WVChromeClient());
        setWebViewClient(new WVClient());
    }

    // 进度显示
    // Progress display.
    private class WVChromeClient extends WebChromeClient {

        // 获得网页的加载进度并显示
        // Get and display the page loading progress.
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress >= 100) {
                mProgressBar.setVisibility(GONE);
            } else {
                if (mProgressBar.getVisibility() == GONE)
                    mProgressBar.setVisibility(VISIBLE);
                mProgressBar.setProgress(newProgress);
            }

            if (mWebViewListener != null) {
                mWebViewListener.onProgressChange(view, newProgress);
            }
            super.onProgressChanged(view, newProgress);
        }

        // 支持javascript的警告框
        // Support JavaScript alert dialogs.
        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            new AlertDialog.Builder(getContext())
                    .setTitle("")
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> result.confirm())
                    .setCancelable(false)
                    .show();
            return true;
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (mWebViewListener != null) {
                mWebViewListener.onSetTitle(title);
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
            if (null == viewFullScreenCallback) {
                return;
            }
            viewFullScreenCallback.onShowCustomView(view, callback);
            mCustomViewCallback = callback;
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if (null == viewFullScreenCallback) {
                return;
            }
            viewFullScreenCallback.onHideCustomView();
            if (mCustomViewCallback == null) {
                return;
            }
            try {
                mCustomViewCallback.onCustomViewHidden();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private class WVClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // 在使得打开网页时不调用系统浏览器， 而是在本WebView中显示
            // Open pages inside this WebView instead of launching the system browser.
            if (url == null) return false;
            try {
                // 微信
                // WeChat.
                if (url.startsWith("weixin://")
                        // 支付宝
                        // Alipay.
                        || url.startsWith("alipays://")
                        // 邮件
                        // Email.
                        || url.startsWith("mailto://")
                        // 电话
                        // Telephone.
                        || url.startsWith("tel://")
                        // 大众点评
                        // Dianping.
                        || url.startsWith("dianping://")
                    // 其他自定义的scheme
                    // Other custom URI schemes.
                ) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    mProgressBar.getContext().startActivity(intent);
                    return true;
                }
            } catch (Exception e) {
                // 防止crash (如果手机上没有安装处理某个scheme开头的url的APP, 会导致crash)
                // Prevent a crash when no installed app can handle a URL with the given scheme.
                // 没有安装该app时，返回true，表示拦截自定义链接，但不跳转，避免弹出上面的错误页面
                // If the app is not installed, return true to consume the custom link without navigating, preventing the error page described above.
                return true;
            }
            view.loadUrl(url);
            return true;
        }

        // webView默认是不处理https请求的，页面显示空白，需要进行如下设置：
        // The original note describes a blank HTTPS page in WebView and the configuration below for handling it.
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // 表示等待证书响应
            // Wait for the certificate response.
            handler.proceed();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mProgressBar.setVisibility(GONE);
            if (mWebViewListener != null) {
                mWebViewListener.onPageFinish(view);
            }
        }

        // 该方法传回了错误码，根据错误类型可以进行不同的错误分类处理
        // The callback supplies an error code so different error types can be handled separately.
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            switch (errorCode) {
                case 404:
                    break;
            }
        }

    }

    private OnWebViewListener mWebViewListener;
    private ViewFullScreenCallback viewFullScreenCallback;

    public void setOnWebViewListener(OnWebViewListener listener) {
        this.mWebViewListener = listener;
    }

    // 进度回调接口
    // Progress callback interface.
    public interface OnWebViewListener {
        void onProgressChange(WebView view, int newProgress);

        void onPageFinish(WebView view);

        void onSetTitle(String title);
    }

    public interface ViewFullScreenCallback {
        void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback);

        void onHideCustomView();
    }

    public void setOnViewFullScreenCallback(ViewFullScreenCallback listener) {
        this.viewFullScreenCallback = listener;
    }

}
