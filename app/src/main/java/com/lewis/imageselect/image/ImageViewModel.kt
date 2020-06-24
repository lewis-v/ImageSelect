package com.lewis.imageselect.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.REQUEST_CROP
import com.yalantis.ucrop.UCrop.RESULT_ERROR
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.internal.entity.CaptureStrategy
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import kotlinx.coroutines.runBlocking
import java.io.File


class ImageViewModel : ViewModel() {

    companion object {
        private const val TAG = "ImageUtil"
        private const val REQUEST_CODE_CHOOSE = 1;
    }

    //当前图片裁剪信息
    private var imageCropInfo: ImageCropInfo? = null
    //当前图片压缩信息
    private var imageCompressionInfo: ImageCompressionInfo? = null

    //选择结果通知
    private val mImageSelectResultLiveData = MutableLiveData<List<Uri>>()
    val imageSelectResultViewModel: LiveData<List<Uri>> = mImageSelectResultLiveData

    //选择错误通知
    private val mImageErrorLiveData = MutableLiveData<String>()
    val imageErrorLiveData: LiveData<String> = mImageErrorLiveData

    fun selectImage(activity: Activity, count: Int = 1, imageCropInfo: ImageCropInfo? = null, imageCompressionInfo: ImageCompressionInfo? = null) {
        this.imageCropInfo = imageCropInfo
        this.imageCompressionInfo = imageCompressionInfo
        Matisse.from(activity)
            .choose(MimeType.ofImage())
            .countable(true)
            .maxSelectable(count)
//            .gridExpectedSize(activity.getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
//            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            .thumbnailScale(0.5f)
            .imageEngine(GlideEngine())
            .capture(true)  // 使用相机，和 captureStrategy 一起使用
            .captureStrategy(CaptureStrategy(true, "com.lewis.imageselect"))
//            .showPreview(false) // Default is `true`
            .forResult(REQUEST_CODE_CHOOSE)
    }

    fun selectImage(fragment: Fragment, count: Int = 1, imageCropInfo: ImageCropInfo? = null, imageCompressionInfo: ImageCompressionInfo? = null) {
        this.imageCropInfo = imageCropInfo
        this.imageCompressionInfo = imageCompressionInfo
        Matisse.from(fragment)
            .choose(MimeType.ofImage())
            .countable(true)
            .maxSelectable(count)
//            .gridExpectedSize(activity.getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
//            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            .thumbnailScale(0.5f)
            .imageEngine(GlideEngine())
            .capture(true)  // 使用相机，和 captureStrategy 一起使用
            .captureStrategy(CaptureStrategy(true, "com.lewis.imageselect"))
//            .showPreview(false) // Default is `true`
            .forResult(REQUEST_CODE_CHOOSE)
    }

    fun onResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        onResult(activity, requestCode, resultCode, data) {
            it.start(activity)
        }
    }

    fun onResult(fragment: Fragment, requestCode: Int, resultCode: Int, data: Intent?) {
        onResult(fragment.requireContext(), requestCode, resultCode, data) {
            it.start(fragment.requireContext(), fragment)
        }
    }

    private fun onResult(
        context: Context,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        startCrop: (UCrop) -> Unit
    ) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_CHOOSE -> {//图片选择结果
                    val urlList = Matisse.obtainResult(data)
                    val tmp = imageCropInfo
                    if (tmp != null && urlList.size == 1) {//需要要裁剪且选择的是1张图的情况进入裁剪
                        val crop = UCrop.of(urlList[0], getCropCacheUri(context))//裁剪之后的临时存储地址
                        if (tmp.hasRatioLimit()) {
                            crop.withAspectRatio(tmp.ratio.x, tmp.ratio.y)//裁剪比例设置
                        }
                        if (tmp.hasSizeLimit()) {
                            crop.withMaxResultSize(tmp.maxSize.x, tmp.maxSize.y)//裁剪最大尺寸设置
                        }
                        startCrop.invoke(crop)
                    } else {
                        selectSuccess(urlList, context)
                    }
                }
                REQUEST_CROP -> {//裁剪结果
                    val resultUri = UCrop.getOutput(data);
                    if (resultUri == null) {
                        Log.e(TAG, "crop err resultUri is null")
                        notifySelectErr("裁剪失败,请重试")
                    } else {
                        selectSuccess(arrayListOf(resultUri), context)
                    }
                }
                RESULT_ERROR -> {//裁剪出错
                    Log.e(TAG, "crop err: ${UCrop.getError(data)}");
                    notifySelectErr("裁剪失败,请重试")
                }
                else -> {
                    Log.w(
                        TAG,
                        "onResult un support requestCode:$requestCode resultCode:$resultCode "
                    )
                }
            }
        }
    }

    private fun getCropCacheUri(context: Context): Uri {
        return Uri.fromFile(
            File(
                context.externalCacheDir,
                "${System.currentTimeMillis()}_crop.jpg"
            )
        )
    }

    private fun selectSuccess(uriList: List<Uri>, context: Context) {
        val tmp = imageCompressionInfo
        if (tmp != null) {
            val comResultList = ArrayList<Uri>()
            uriList.forEach {
                //压缩,开启携程在子线程压缩,并等待压缩完成
                val result = runBlocking {
                    if (it.toString().startsWith("content")) {
                        PathUtils.getPath(context, it)?.let {
                            Compressor.compress(context, File(it.toString())) {
                                default(format = Bitmap.CompressFormat.JPEG)
                            }
                        }
                    } else {
                        Compressor.compress(context, it.toFile()) {
                            default(format = Bitmap.CompressFormat.JPEG)
                        }
                    }
                }
                comResultList.add(Uri.fromFile(result))
            }
            notifySelectResult(comResultList)
        } else {
            notifySelectResult(uriList)
        }
    }

    private fun notifySelectResult(uriList: List<Uri>) {
        Log.i(TAG, "notifySelectResult $uriList")
        mImageSelectResultLiveData.postValue(uriList)
    }

    private fun notifySelectErr(errMsg: String) {
        Log.i(TAG, "notifySelectErr :$errMsg")
        mImageErrorLiveData.postValue(errMsg)
    }

}