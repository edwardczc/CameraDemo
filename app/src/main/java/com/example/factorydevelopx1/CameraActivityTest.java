package com.example.factorydevelopx1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraActivityTest extends AppCompatActivity implements View.OnClickListener {
    public   static final String TAG = "jack";
    private  static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0,90);//手机自然手持状态,home按键在底部
        ORIENTATIONS.append(Surface.ROTATION_90,0);//home按键在右侧
        ORIENTATIONS.append(Surface.ROTATION_180,270);//home按键在左侧
        ORIENTATIONS.append(Surface.ROTATION_270,180);//home按键在顶部
    }
    private  AutoFitTextureView textureView;
    private  String mCameraId = "0";
    private  CameraDevice cameraDevice;

    private Size previewSize;
    private CaptureRequest.Builder previewRequestBuilder;

    private CaptureRequest previewRequest;

    private CameraCaptureSession captureSession;

    private ImageReader imageReader;
    private ImageView takepicture;
    private ImageView picture_show;

    private Uri mPicture_uri;
    private CameraManager manager;

    private  final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG,"onSurfaceTextureAvailable");
            openCamrea(width,height);

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG,"onSurfaceTextureSizeChanged");

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG,"onSurfaceTextureDestroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.d(TAG,"onSurfaceTextureUpdated");

        }
    };


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG,"CameraDevice.StateCallback onOpened");
            CameraActivityTest.this.cameraDevice = camera;
            createCameraPreviewSession();


        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG,"CameraDevice.StateCallback onDisconnected");
            camera.close();
            CameraActivityTest.this.cameraDevice = null;

        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG,"CameraDevice.StateCallback onError");
            camera.close();
            CameraActivityTest.this.cameraDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_camera);
        textureView = findViewById(R.id.texture);
        Log.d(TAG,"onCreate11111");
        textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        takepicture = (ImageView)findViewById(R.id.take_picture);
        takepicture.setOnClickListener(this);
        picture_show = (ImageView)findViewById(R.id.picture_show);
        picture_show.setOnClickListener(this);
        Log.d(TAG,"onCreate22222");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(textureView != null){
            Log.d(TAG,"onResume111111111");
            openCamrea(textureView.getWidth(),textureView.getHeight());
        }else {
            Log.d(TAG,"onResume2222222222");
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.take_picture:
                captureStillPicture();
                break;
            case R.id.picture_show:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/*");
                intent.setData(mPicture_uri);
                startActivity(Intent.createChooser(intent, "image/*"));
            default:
        }

    }


    //打开摄像头,默认打开后置摄像头 0
    private void openCamrea(int width,int height){
        Log.d(TAG,"openCamrea width=="+width+"  height=="+height);
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            this.requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else {
            SetUpCameraOutputs(width,height);
             manager = (CameraManager)this.getSystemService(Context.CAMERA_SERVICE);

            try{
                manager.openCamera(mCameraId,stateCallback,null);
            }catch (CameraAccessException e){
                e.printStackTrace();
            }
        }
    }

    private void SetUpCameraOutputs(int width,int height){
        Log.d(TAG,"SetUpCameraOutputs111111==>"+"width=="+width+" height=="+height);
        CameraManager cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try{
            //获取指定摄像头的特性
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);
            //获取摄像头支持的配置属性
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Log.d(TAG,"SetUpCameraOutputs=="+map);
            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),new CompareSizeByArea());
            Log.d(TAG,"SetUpCameraOutputs222222==> width=="+largest.getWidth()+"  height=="+largest.getHeight());
            imageReader = ImageReader.newInstance(largest.getWidth(),largest.getHeight(),ImageFormat.JPEG,2);

            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                //当照片数据可用时激发该方法
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d(TAG,"onImageAvailable");
                    Image  image = reader.acquireNextImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    File file = new File(Environment.getExternalStorageDirectory(),"pic.jpg");
                    FileOutputStream fileOutputStream ;

                    //如果之前的拍摄的照片存在,需将其删除
                    if(file.exists()){
                        file.delete();
                    }
                    buffer.get(bytes);
                    try {
                        fileOutputStream = new FileOutputStream(file);
                        fileOutputStream.write(bytes);
                        if(file != null){
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                            picture_show.setImageBitmap(bitmap);//显示拍摄的照片
                            //兼容性处理,Android 6.0 之后不允许uri 以file的形式传播,必须采用fileprovider进行转换
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                                mPicture_uri = FileProvider.getUriForFile(CameraActivityTest.this,"com.example.factorydevelopx1.CameraActivityTest",file);
                            }else {
                                mPicture_uri = Uri.fromFile(file);
                            }
                        }
                        fileOutputStream.close();
                        Toast.makeText(CameraActivityTest.this,"保存:"+file,Toast.LENGTH_SHORT).show();
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        image.close();
                    }
                }
            },null);
            Log.d(TAG,"SetUpCameraOutputs width=="+width+"  height=="+height+ "  largest=="+largest+"  map.getOutputSizes(SurfaceTexture.class)"+map.getOutputSizes(SurfaceTexture.class));
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),width,height,largest);

            int oritention = getResources().getConfiguration().orientation;
            Log.d(TAG,"SetUpCameraOutputs oritention=="+oritention);
            Log.d(TAG,"SetUpCameraOutputs previewSize==> previewSize.getWidth()=="+previewSize.getWidth()+"  previewSize.getHeight()=="+previewSize.getHeight());
            if(oritention == Configuration.ORIENTATION_LANDSCAPE){
                Log.d(TAG,"SetUpCameraOutputs textureView.setAspectRadio11111 ");
                textureView.setAspectRadio(previewSize.getWidth(),previewSize.getHeight());
            }else {
                Log.d(TAG,"SetUpCameraOutputs textureView.setAspectRadio2222");
                //textureView.setAspectRadio(previewSize.getHeight(),previewSize.getWidth());//该段若启用,则会根据传递的比例previewSize.getHeight() previewSize.getWidth()进行屏幕自适应
                //textureView.setAspectRadio(24,38);
            }

        }catch(CameraAccessException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static Size chooseOptimalSize(Size[]choise,int width,int height,Size aspectRatio){
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for(Size option :choise){
            if(option.getHeight() == option.getWidth() *h/w && option.getWidth() >= width && option.getHeight() >=height){
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0){
            return Collections.min(bigEnough,new CompareSizeByArea());
        }else {
            Log.e("jack","can not get correct size");
            return choise[0];
        }
    }
    //为Size定义一个比较器
    static class CompareSizeByArea implements Comparator<Size>{

        @Override
        //强转为long保证不会发生溢出
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long)lhs.getWidth()*lhs.getHeight()-(long)rhs.getWidth()*rhs.getHeight());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openCamrea(textureView.getWidth(),textureView.getHeight());
                }else{
                //todo 弹出一个对话框请求再次申请权限并解释使用权限的原因
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    private void createCameraPreviewSession(){

        try {
            Log.d("jack","createCameraPreviewSession11111111");
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(),previewSize.getHeight());
            Surface surface = new Surface(texture);
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            Log.d("jack","createCameraPreviewSession2222222");
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d("jack","createCameraPreviewSession33333333");
                    if( session == null){
                        return;
                    }
                    captureSession = session;
                    try {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        previewRequest = previewRequestBuilder.build();
                        captureSession.setRepeatingRequest(previewRequest,null,null);
                        Log.d("jack","createCameraPreviewSession444444444");
                    }catch (CameraAccessException e){
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(CameraActivityTest.this,"配置失败",Toast.LENGTH_SHORT).show();

                }
            },null);
        }catch (Exception e){
            e.printStackTrace();
        }



    }

    //关闭相机,释放资源
    private void closeCamera(){
        try {
            if(cameraDevice != null){
               cameraDevice.close();
               cameraDevice = null;
            }
            if(captureSession != null){
                captureSession.close();
                captureSession = null;
            }
            if(imageReader != null){
                imageReader.close();
                imageReader = null;
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void captureStillPicture(){
        try {
            if(cameraDevice == null){
                return;
            }
            //创建作为拍照的CaptureRequest.Builder
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //设置自动曝光
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_TORCH);
            //captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.);
            //根据设备方向计算设置照片方向
            int oritention = getWindowManager().getDefaultDisplay().getRotation();
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(oritention));
            //停止连续取景
            captureSession.stopRepeating();
            captureSession.capture(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                //拍照完成激发该方法
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,CaptureRequest request,TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    try{
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        captureSession.setRepeatingRequest(previewRequest,null,null);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            },null);


        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
