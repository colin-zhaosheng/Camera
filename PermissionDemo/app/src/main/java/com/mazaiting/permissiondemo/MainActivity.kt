package com.mazaiting.permissiondemo

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.tbruyelle.rxpermissions2.RxPermissions

class MainActivity : AppCompatActivity() {
  // 定义权限类
  private lateinit var mRxPermission: RxPermissions
  
  // 存储静态量
  companion object {
    /**权限设置页返回*/
    val REQUEST_PERMISSION_SETTING = 0x101
  }
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    mRxPermission = RxPermissions(this)
  }
  
  
  /**
   * 拍照按钮
   */
  fun takePicture(view: View) {
    mRxPermission
            .requestEachCombined(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .subscribe { permission ->
              when {
                permission.granted -> openCamera()
                permission.shouldShowRequestPermissionRationale -> showSetting()
                else -> {
                  Toast.makeText(this, "被拒绝", Toast.LENGTH_SHORT).show()
                  showSetting()
                }
              }
            }
  }
  
  /**
   * 显示权限设置页
   */
  private fun showSetting() {
    // 已经禁止提示
    AlertDialog
            .Builder(this)
            .setTitle("提示")
            .setMessage("权限已拒绝，是否需要重新开启")
            .setPositiveButton("确定") { dialog: DialogInterface?, which: Int ->
              // 设置防止出现不再提示页面,进入权限管理页面
              val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
              val uri = Uri.fromParts("package", packageName, null)
              intent.data = uri
              startActivityForResult(intent, REQUEST_PERMISSION_SETTING)
            }
            .setNegativeButton("取消", null)
            .create()
            .show()
  }
  
  /**
   * 打开相机
   */
  private fun openCamera() {
    val openCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    startActivity(openCameraIntent)
  }
}
