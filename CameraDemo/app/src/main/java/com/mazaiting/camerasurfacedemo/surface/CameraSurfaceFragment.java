package com.mazaiting.camerasurfacedemo.surface;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.mazaiting.camerasurfacedemo.R;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Android 5.0 使用Camera2 照相
 * 使用SurfaceView显示
 * 主要步骤：
 * 1. 获得摄像头管理器CameraManager mCameraManager，mCameraManager.openCamera()来打开摄像头
 * 2. 指定要打开的摄像头，并创建openCamera()所需要的CameraDevice.StateCallback stateCallback
 * 3. 在CameraDevice.StateCallback stateCallback中调用takePreview()，这个方法中，使用CaptureRequest.Builder创建预览需要的CameraRequest，并初始化了CameraCaptureSession，最后调用了setRepeatingRequest(previewRequest, null, childHandler)进行了预览
 * 4. 点击拍照按钮，调用takePicture()，这个方法内，最终调用了capture(mCaptureRequest, null, childHandler)
 * 5. 在new ImageReader.OnImageAvailableListener(){}回调方法中，将拍照拿到的图片进行展示
 */
public class CameraSurfaceFragment extends Fragment {
  /**照相机设备请求码*/
  private static final int REQUEST_CAMERA_CODE = 0x100;
  /**拍照*/
  private Button mBtnTake;
  /**显示拍照好的图片*/
  private ImageView mIvShow;
  /**预览窗口*/
  private SurfaceView mSurfaceView;
  /**预览窗口Holder*/
  private SurfaceHolder mSurfaceHolder;
  /**子线程Handler*/
  private Handler mChildHandler;
  /**主线程Handler*/
  private Handler mMainHandler;
  /**照相机ID，标识前置，后置*/
  private String mCameraId;
  /**图片读取器*/
  private ImageReader mImageReader;
  /**摄像头管理者*/
  private CameraManager mCameraManager;
  /**照相机设备*/
  private CameraDevice mCameraDevice;
  /**照相会话*/
  private CameraCaptureSession mCameraCaptureSession;
  /**方向列表*/
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
  
  /**
   * 摄像头状态监听
   */
  private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(@NonNull CameraDevice camera) {
      // 打开摄像头
      mCameraDevice = camera;
      // 开启预览
      takePreview();
    }
  
    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
      // 关闭摄像头
      if (null != mCameraDevice) {
        // 关闭摄像头
        mCameraDevice.close();
        mCameraDevice = null;
      }
    }
  
    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
      // 摄像头异常
      Toast.makeText(getContext(), "摄像头开启失败", Toast.LENGTH_SHORT).show();
    }
  };
  
  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.fragment_camera_surface, container, false);
    initView(root);
    initListener();
    return root;
  }
  
  /**
   * 初始化View
   */
  private void initView(View view) {
    // 绑定View
    mIvShow = view.findViewById(R.id.iv_show);
    mBtnTake = view.findViewById(R.id.btn_take);
    mSurfaceView = view.findViewById(R.id.sv_camera);
    // 获取Holder
    mSurfaceHolder = mSurfaceView.getHolder();
    // 设置屏幕常量
    mSurfaceHolder.setKeepScreenOn(true);
    // 设置SurfaceView回调
    mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
      @Override
      public void surfaceCreated(SurfaceHolder holder) {
        // SurfaceView 创建
        initCamera();
      }
  
      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // SurfaceView 改变
      }
  
      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {
        // SurfaceView 销毁
        // 销毁照相机设备
        if (null != mCameraDevice) {
          mCameraDevice.close();
          mCameraDevice = null;
        }
      }
    });
  }
  
  /**
   * 初始化监听器
   */
  private void initListener() {
    mBtnTake.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        takePicture();
      }
      
    });
  }
  
  /**
   * 初始化照相机
   */
  private void initCamera() {
    // 创建Handler线程并启动
    HandlerThread handlerThread = new HandlerThread("Camera");
    handlerThread.start();
    // 创建子线程Handler
    mChildHandler = new Handler(handlerThread.getLooper());
    // 创建主线程Handler
    mMainHandler = new Handler(Looper.getMainLooper());
    // 设置后置摄像头ID
    mCameraId = String.valueOf(CameraCharacteristics.LENS_FACING_FRONT);
    // 创建图片读取器
    mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1);
    // 图片读取器设置图片可用监听
    mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
      @Override
      public void onImageAvailable(ImageReader reader) {
        showImage(reader);
      }
    }, mMainHandler);
    // 获取摄像头管理
    mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
    // 打开摄像头
    try {
      if (ActivityCompat.checkSelfPermission
              (getContext(), Manifest.permission.CAMERA)
              != PackageManager.PERMISSION_GRANTED) {
        // 申请权限
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
      } else {
        // 打开摄像头
        mCameraManager.openCamera(mCameraId, mStateCallback, mMainHandler);
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (REQUEST_CAMERA_CODE == requestCode) {
      // 权限允许
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        try {
          if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
          }
          mCameraManager.openCamera(mCameraId, mStateCallback, mMainHandler);
        } catch (CameraAccessException e) {
          e.printStackTrace();
        }
      } else {
        // 权限拒绝
        Toast.makeText(getContext(), "无权限", Toast.LENGTH_SHORT).show();
      }
    }
  }
  
  /**
   * 图片可用后，读取并显示图片
   * @param reader 图片读取器
   */
  private void showImage(ImageReader reader) {
    // 拿到图片数据
    Image image = reader.acquireNextImage();
    // 获取字节缓冲
    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
    // 创建与缓冲区相同的字节数组
    byte[] bytes = new byte[buffer.remaining()];
    // 将数据读取字节数组
    buffer.get(bytes);
    // 创建图片
    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    // 显示图片
    if (null != bitmap) {
      mIvShow.setImageBitmap(bitmap);
    }
  }
  
  /**
   * 预览
   */
  private void takePreview() {
    try {
      // 创建预览需要的CaptureRequest.Builder
      final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      // 将SurfaceView的surface作为Builder的目标
      builder.addTarget(mSurfaceHolder.getSurface());
      // 创建CameraCaptureSession,该对象负责管理处理预览请求和拍照请求
      mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
          // 检测设备是否为空
          if (null == mCameraDevice) return;
          // 配置
          // 当摄像头已经准备好时，开始显示预览
          mCameraCaptureSession = session;
          try {
            // 自动对焦
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 打开闪光灯
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 显示预览
            CaptureRequest request = builder.build();
            // 会话设置重复请求
            mCameraCaptureSession.setRepeatingRequest(request, null, mChildHandler);
          } catch (CameraAccessException e) {
            e.printStackTrace();
          }
        }
  
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
          Toast.makeText(getContext(), "配置失败", Toast.LENGTH_SHORT).show();
        }
      }, mChildHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * 拍照
   */
  private void takePicture() {
    // 判断设备是否为空
    if (null == mCameraDevice) return;
    // 创建拍照需要的CaptureRequest.Builder
    final CaptureRequest.Builder builder;
    try {
      // 创建拍照请求
      builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      // 将imageReader的surface作为CaptureRequest.Builder的木白哦
      builder.addTarget(mImageReader.getSurface());
      // 自动对焦
      builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
      // 自动曝光
      builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
      // 获取手机方向
      int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
      // 根据设备方向计算设置照片的方向
      builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
      // 拍照
      CaptureRequest request = builder.build();
      // 拍照会话执行拍照
      mCameraCaptureSession.capture(request, null, mChildHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
}
