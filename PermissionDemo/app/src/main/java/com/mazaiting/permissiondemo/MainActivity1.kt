package com.mazaiting.permissiondemo

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast

class MainActivity1 : AppCompatActivity() {
  
  // 存储静态量
  companion object {
    /**权限请求*/
    val REQUEST_PERMISSION = 0x100
    /**权限设置页返回*/
    val REQUEST_PERMISSION_SETTING = 0x101
  }
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }
  
  
  /**
   * 拍照按钮
   */
  fun takePicture(view: View) {
    // 检测权限
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      // 用户拒绝了权限,并且点击了不再提醒
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
        // 已经禁止提示
        AlertDialog.Builder(this)
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
      } else {
        // 无权限
        ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_PERMISSION
        )
      }
    } else {
      // 有权限
      openCamera()
    }
  }
  
  /**
   * 打开相机
   */
  private fun openCamera() {
    val openCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    startActivity(openCameraIntent)
  }
  
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    // 判断请求码
    if (REQUEST_PERMISSION == requestCode) {
      // 判断grantResults数组不为空
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // 用户同意授权
        openCamera()
      } else {
        // 用户拒绝授权
        Toast.makeText(this, "用户拒绝了授权", Toast.LENGTH_SHORT).show()
      }
    }
  }
}