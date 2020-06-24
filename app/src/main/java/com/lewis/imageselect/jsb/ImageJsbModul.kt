package com.lewis.imageselect.jsb

import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.core.net.toFile
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.lewis.imageselect.image.ImageCompressionInfo
import com.lewis.imageselect.image.ImageCropInfo
import com.lewis.imageselect.image.ImageViewModel
import com.lewis.imageselect.image.PathUtils
import com.lewis.imageselect.util.PermissionUtil
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class ImageJsbModul(val activity: FragmentActivity, val web: WebView) {

    companion object {
        private const val TAG = "ImageJsbModul"
    }

    //选择结果回调方法名
    private var imageSelectCallback: String? = null
    //当前选择数量
    private var selectCount: Int = 0
    //当前裁剪信息
    private var cropInfo: ImageCropInfo? = null
    //当前压缩信息
    private var compressionInfo: ImageCompressionInfo? = null

    private val imageViewModel =
        ViewModelProvider(
            activity,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
        ).get(ImageViewModel::class.java)

    init {
        imageViewModel.imageSelectResultViewModel.observe(activity, Observer {
            selectFinish(it)//选择结果回调
        })
        imageViewModel.imageErrorLiveData.observe(activity, Observer {
            //选择错误toast
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
        })
    }

    /**
     * jsb方法,选择图片
     * window.ImageSelect.selectImage(num, '{"ratio":{"x":16.0,"y":9.0}}',true,"callBack");
     */
    @JavascriptInterface
    fun selectImage(
        selectCount: Int,
        cropInfo: String?,
        isCompression: Boolean,
        callback: String?
    ) {
        Log.i(
            TAG,
            "selectImage selectCount:$selectCount cropInfo:$cropInfo isCompression:$isCompression callback:$callback"
        )
        if (selectCount in 1..9) {
            imageSelectCallback = callback
            this.selectCount = selectCount
            cropInfo?.let {
                val json = JSONObject(it)
                val info = ImageCropInfo()
                if (selectCount == 1) {
                    json.optJSONObject("ratio")?.let {
                        val x = it.optDouble("x", 0.0)
                        val y = it.optDouble("y", 0.0)
                        if (x > 0 && y > 0) {
                            info.ratio.x = x.toFloat()
                            info.ratio.y = y.toFloat()
                        }
                    }
                }
                json.optJSONObject("maxSize")?.let {
                    val x = it.optInt("x", 0)
                    val y = it.optInt("y", 0)
                    if (x > 0 && y > 0) {
                        info.maxSize.x = x
                        info.maxSize.y = y
                    }
                }
                this.cropInfo = info
                if (isCompression) {
                    compressionInfo = ImageCompressionInfo()
                } else {
                    compressionInfo = null
                }
            }

            notifySelectImage()
        } else {
            Log.e(TAG, "selectImage not support $selectCount count")
        }
    }

    private fun selectFinish(uriList: List<Uri>) {
        runBlocking {
            imageSelectCallback?.let {
                val resultList = ArrayList<String>()
                uriList.forEach {
                    //选择的结果转为base64回传给web,这里建议最好是上传到对象存储,然后将图片链接传给web
                    val result = if (it.toString().startsWith("content")) {
                        imageToBase64(File(PathUtils.getPath(activity, it) ?: ""))
                    } else {
                        imageToBase64(uriList[0].toFile())
                    }
                    if (result?.isNotBlank() == true) {
                        resultList.add(result)
                    }
                }
                Log.i("selectFinish", "select result :${resultList.size}")
                web.loadUrl("javascript:${it}('${JSONArray(resultList)}')");
            }
            imageSelectCallback = null
        }
    }

    private fun notifySelectImage() {
        if (PermissionUtil.checkAllPermission(activity)) {
            imageViewModel.selectImage(activity, selectCount, cropInfo, compressionInfo)
        }
    }

    /**
     * 文件转base64
     */
    private fun imageToBase64(file: File): String? {
        Log.i("imageToBase64", "path : $file")
        var result = ""
        try {
            result = String(Base64.encode(file.readBytes(), Base64.NO_WRAP))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun onGetPermission() {
        imageSelectCallback?.let {
            imageViewModel.selectImage(activity, selectCount, cropInfo, compressionInfo)
        }
    }
}