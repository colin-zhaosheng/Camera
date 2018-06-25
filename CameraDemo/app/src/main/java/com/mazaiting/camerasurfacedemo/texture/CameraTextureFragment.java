package com.mazaiting.camerasurfacedemo.texture;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.mazaiting.camerasurfacedemo.R;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Camera2 与 Texture使用
 */
public class CameraTextureFragment extends Fragment {
  /**相机权限请求标识*/
  private static final int REQUEST_CAMERA_CODE = 0x100;
  /**预览*/
  private TextureView mTextureView;
  /**拍照按钮*/
  private Button mBtnTake;
  /**图片*/
  private ImageView mImageView;
  /**照相机ID，标识前置后置*/
  private String mCameraId;
  /**相机尺寸*/
  private Size mCaptureSize;
  /**图像读取者*/
  private ImageReader mImageReader;
  /**图像主线程Handler*/
  private Handler mCameraHandler;
  /**相机设备*/
  private CameraDevice mCameraDevice;
  /**预览大小*/
  private Size mPreviewSize;
  /**相机请求*/
  private CaptureRequest.Builder mCameraCaptureBuilder;
  /**相机拍照捕获会话*/
  private CameraCaptureSession mCameraCaptureSession;
  /**相机管理者*/
  private CameraManager mCameraManager;
  /**相机设备状态回调*/
  private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(@NonNull CameraDevice camera) {
      // 打开
      mCameraDevice = camera;
      // 开始预览
      takePreview();
    }
  
    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
      // 断开连接
      camera.close();
      mCameraDevice = null;
    }
  
    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
      // 异常
      camera.close();
      mCameraDevice = null;
    }
  };
  
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.fragment_camera_texture, container, false);
    initView(root);
    initListener();
    return root;
  }
  
  /**
   * 初始化View
   */
  private void initView(View view) {
    // 初始化View
    mImageView = view.findViewById(R.id.iv_show);
    mTextureView = view.findViewById(R.id.tv_camera);
    mBtnTake = view.findViewById(R.id.btn_take);
  }
  
  @Override
  public void onResume() {
    super.onResume();
    mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
      @Override
      public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // SurfaceTexture可用
        // 设置相机参数并打开相机
        setUpCamera(width, height);
        openCamera();
      }
  
      @Override
      public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // SurfaceTexture大小改变
      }
  
      @Override
      public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // SurfaceTexture 销毁
        return false;
      }
  
      @Override
      public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // SurfaceTexture 更新
      }
    });
  }
  
  /**
   * 打开相机
   */
  private void openCamera() {
    // 获取照相机管理者
    mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
    try {
      if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
        return;
      }
      // 打开相机
      mCameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
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
          mCameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
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
   * 设置相机参数
   * @param width 宽度
   * @param height 高度
   */
  private void setUpCamera(int width, int height) {
    // 创建Handler
    mCameraHandler = new Handler(Looper.getMainLooper());
    // 获取摄像头的管理者
    CameraManager cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
    try {
      // 遍历所有摄像头
      for (String cameraId : cameraManager.getCameraIdList()) {
        // 相机特性
        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        // 获取摄像头是前置还是后置
        Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        // 此处默认打开后置摄像头
        if (null != facing && CameraCharacteristics.LENS_FACING_FRONT == facing)  continue;
        // 获取StreamConfigurationMap，管理摄像头支持的所有输出格式和尺寸
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        assert map != null;
        // 根据TextureView的尺寸设置预览尺寸
        mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
        // 获取相机支持的最大拍照尺寸
        mCaptureSize = Collections.max(
                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                  @Override
                  public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                  }
                });
        // 此处ImageReader用于拍照所需
        setupImageReader();
        // 为摄像头赋值
        mCameraId = cameraId;
        break;
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * 设置ImageReader
   */
  private void setupImageReader() {
    // 2代表ImageReader中最多可以获取两帧图像流
    mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.JPEG, 1);
    // 设置图像可用监听
    mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
      @Override
      public void onImageAvailable(ImageReader reader) {
        // 获取图片
        final Image image = reader.acquireNextImage();
        // 提交任务，保存图片
        mCameraHandler.post(new ImageSaver(image));
        // 更新UI
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            // 获取字节缓冲区
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            // 创建数组之前调用此方法，恢复默认设置
            buffer.rewind();
            // 创建与缓冲区内容大小相同的数组
            byte[] bytes = new byte[buffer.remaining()];
            // 从缓冲区存入字节数组,读取完成之后position在末尾
            buffer.get(bytes);
            // 获取Bitmap图像
            final Bitmap bitmap =  BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            // 显示
            if (null != bitmap) {
              mImageView.setImageBitmap(bitmap);
            }
            
          }
        });
      }
    }, mCameraHandler);
  }
  
  /**
   * 选择SizeMap中大于并且最接近width和height的size
   * @param sizeMap 可选的尺寸
   * @param width 宽
   * @param height 高
   * @return 最接近width和height的size
   */
  private Size getOptimalSize(Size[] sizeMap, int width, int height) {
    // 创建列表
    List<Size> sizeList = new ArrayList<>();
    // 遍历
    for (Size option : sizeMap) {
      // 判断宽度是否大于高度
      if (width > height) {
        if (option.getWidth() > width && option.getHeight() > height) {
          sizeList.add(option);
        }
      } else {
        if (option.getWidth() > height && option.getHeight() > width) {
          sizeList.add(option);
        }
      }
    }
    // 判断存储Size的列表是否有数据
    if (sizeList.size() > 0) {
      return Collections.min(sizeList, new Comparator<Size>() {
        @Override
        public int compare(Size lhs, Size rhs) {
          return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
        }
      });
    }
    return sizeMap[0];
  }
  
  /**
   * 设置监听
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
   * 预览
   */
  private void takePreview(){
    // 获取SurfaceTexture
    SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
    // 设置默认的缓冲大小
    surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
    // 创建Surface
    Surface previewSurface = new Surface(surfaceTexture);
    try {
      // 创建预览请求
      mCameraCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      // 将previewSurface添加到预览请求中
      mCameraCaptureBuilder.addTarget(previewSurface);
      // 创建会话
      mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
          try {
            // 配置
            CaptureRequest captureRequest = mCameraCaptureBuilder.build();
            // 設置session
            mCameraCaptureSession = session;
            // 设置重复预览请求
            mCameraCaptureSession.setRepeatingRequest(captureRequest, null, mCameraHandler);
          } catch (CameraAccessException e) {
            e.printStackTrace();
          }
        }
  
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
          // 配置失败
        }
      }, mCameraHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * 拍照
   */
  private void takePicture() {
    try {
      // 设置触发
      mCameraCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
      mCameraCaptureBuilder.addTarget(mImageReader.getSurface());
      // 拍照
      mCameraCaptureSession.capture(mCameraCaptureBuilder.build(), null, mCameraHandler);
    } catch (CameraAccessException e) {
      Toast.makeText(getActivity(), "异常", Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    }
  }
  
  /**
   * 保存图片任务
   */
  private class ImageSaver implements Runnable {
    /**图像*/
    private Image mImage;
    ImageSaver(Image image) {
      this.mImage = image;
    }
  
    @Override
    public void run() {
      // 获取字节缓冲区
      ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
      // 创建数组之前调用此方法，恢复默认设置--重要
      buffer.rewind();
      // 创建与字节缓冲区大小相同的字节数组
      byte[] data = new byte[buffer.remaining()];
      // 将数据读取字节数组
      buffer.get(data);
      // 获取缓存路径
      String path = getActivity().getExternalCacheDir().getPath();
      // 获取时间戳
      String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
      // 文件名
      String fileName = path + "/IMG_" + timeStamp + ".jpg";
      // 创建文件输出流
      FileOutputStream fos = null;
      try {
        // 初始化文件输出流
        fos = new FileOutputStream(fileName);
        // 将数据写入文件
        fos.write(data, 0, data.length);
        // 刷新缓冲区
        fos.flush();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (null != fos) {
          try {
            fos.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }
}
