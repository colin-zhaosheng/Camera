package com.mazaiting.camerasurfacedemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mazaiting.camerasurfacedemo.surface.CameraSurfaceFragment;
import com.mazaiting.camerasurfacedemo.texture.CameraTextureFragment;
import com.mazaiting.camerasurfacedemo.texture.CameraTextureMoreFunFragment;

public class MainActivity extends AppCompatActivity {
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    getSupportFragmentManager()
            .beginTransaction()
            // SurfaceView
//            .replace(R.id.fl_content, new CameraSurfaceFragment(), "Surface")
            // Texture
//            .replace(R.id.fl_content, new CameraTextureFragment(), "Texture")
            // More Function
            .replace(R.id.fl_content, new CameraTextureMoreFunFragment(), "Texture")
            .commit();
  }
}
