package com.lewis.imageselect

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.lewis.imageselect.image.ImageCompressionInfo
import com.lewis.imageselect.image.ImageCropInfo
import com.lewis.imageselect.image.ImageViewModel
import com.lewis.imageselect.util.PermissionUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val imageViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(ImageViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        imageViewModel.imageErrorLiveData.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        })

        imageViewModel.imageSelectResultViewModel.observe(this, Observer {
            //图片选择结果
            Toast.makeText(this, "select success ${it.size}", Toast.LENGTH_SHORT).show()
        })

        bt_select.setOnClickListener {
            if (PermissionUtil.checkAllPermission(this)) {
                //选择一张图片,不裁剪
                imageViewModel.selectImage(this, 1, null, ImageCompressionInfo())
            }
        }
        bt_crop.setOnClickListener {
            if (PermissionUtil.checkAllPermission(this)) {
                //选择1张图片,裁剪
                imageViewModel.selectImage(this, 1, ImageCropInfo(), ImageCompressionInfo())
            }
        }
        bt_jump.setOnClickListener {
            val text = et.text.toString()
            if (text.isNotBlank()) {
                val intent = Intent(this, WebActivity::class.java)
                intent.putExtra(WebActivity.BUNDLE_URI, text)
                Log.i("Main", "startActivity $text")
                startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.isNotEmpty()) {
            imageViewModel.selectImage(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        imageViewModel.onResult(this, requestCode, resultCode, data)
    }
}

 

