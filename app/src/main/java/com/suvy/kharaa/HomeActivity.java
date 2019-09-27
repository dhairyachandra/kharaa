package com.suvy.kharaa;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {

  private static final String TAG = HomeActivity.class.getSimpleName();
  private static final int INPUT_FILE_REQUEST_CODE = 1;
  private static final int FILECHOOSER_RESULTCODE = 1;
  private ValueCallback<Uri[]> mFilePathCallback;
  private String mCameraPhotoPath;
  private ValueCallback<Uri> mUploadMessage;
  private Uri mCapturedImageURI = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);
    if (checkConnection()) {
      loadWebView();
    } else {
      dialog();
    }
  }

  void loadWebView() {
    WebView webView = (WebView) findViewById(R.id.myWebView);
    webView.loadUrl("http://www.kharaa.in");
    webView.setWebViewClient(new WebViewClient());
    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);

    webView.setWebChromeClient(new WebChromeClient() {


      // For Android 5.0
      @SuppressLint("NewApi")
      public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
        // Double check that we don't have any existing callbacks
        if (mFilePathCallback != null) {
          mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePath;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
          // Create the File where the photo should go
          File photoFile = null;
          try {
            photoFile = createImageFile();
            takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
          } catch (IOException ex) {
            // Error occurred while creating the File
            Log.e(TAG, "Unable to create Image File", ex);
          }
          // Continue only if the File was successfully created
          if (photoFile != null) {
            mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                       Uri.fromFile(photoFile));
          } else {
            takePictureIntent = null;
          }
        }
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");
        Intent[] intentArray;
        if (takePictureIntent != null) {
          intentArray = new Intent[]{takePictureIntent};
        } else {
          intentArray = new Intent[0];
        }
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
        return true;
      }

      //openFileChooser for other Android versions
      public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                  String acceptType,
                                  String capture) {
        openFileChooser(uploadMsg, acceptType);
      }

      public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        mUploadMessage = uploadMsg;
        // Create AndroidExampleFolder at sdcard
        // Create AndroidExampleFolder at sdcard
        File imageStorageDir = new File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES)
            , "AndroidExampleFolder");
        if (!imageStorageDir.exists()) {
          // Create AndroidExampleFolder at sdcard
          imageStorageDir.mkdirs();
        }
        // Create camera captured image file path and name
        File file = new File(
            imageStorageDir + File.separator + "IMG_"
                + String.valueOf(System.currentTimeMillis())
                + ".jpg");
        mCapturedImageURI = Uri.fromFile(file);
        // Camera capture image intent
        final Intent captureIntent = new Intent(
            android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        // Create file chooser intent
        Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
        // Set camera intent to file chooser
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
            , new Parcelable[]{captureIntent});
        // On select image call onActivityResult method of activity
        startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
      }
    });

  }

  boolean checkConnection() {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    if (activeNetworkInfo != null) {
      return true;
    }
    return false;
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  private File createImageFile() throws IOException {
    // Create an image file name
    File imageFile = null;
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES);
    File imagesFolder = new File(storageDir, "images");
    if (!imagesFolder.exists()) {
      if (imagesFolder.mkdirs()) {
        imageFile = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
        );
      } else {
        imageFile = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
        );
      }
    }

    return imageFile;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
        super.onActivityResult(requestCode, resultCode, data);
        return;
      }
      Uri[] results = null;
      // Check that the response is a good one
      if (resultCode == Activity.RESULT_OK) {
        if (data == null) {
          // If there is not data, then we may have taken a photo
          if (mCameraPhotoPath != null) {
            results = new Uri[]{Uri.parse(mCameraPhotoPath)};
          }
        } else {
          String dataString = data.getDataString();
          if (dataString != null) {
            results = new Uri[]{Uri.parse(dataString)};
          }
        }
      }
      mFilePathCallback.onReceiveValue(results);
      mFilePathCallback = null;
    } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
      if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
        super.onActivityResult(requestCode, resultCode, data);
        return;
      }
      if (requestCode == FILECHOOSER_RESULTCODE) {
        if (null == this.mUploadMessage) {
          return;
        }
        Uri result = null;
        try {
          if (resultCode != RESULT_OK) {
            result = null;
          } else {
            // retrieve from the private variable if the intent is null
            result = data == null ? mCapturedImageURI : data.getData();
          }
        } catch (Exception e) {
          Toast.makeText(getApplicationContext(), "activity :" + e,
                         Toast.LENGTH_LONG).show();
        }
        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;
      }
    }
    return;
  }

  void dialog() {
    AlertDialog alertDialog = new AlertDialog.Builder(HomeActivity.this).create();
    alertDialog.setTitle("No Internet");
    alertDialog.setMessage("Please Check your Internent Connection!");
    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                          new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                              dialog.dismiss();
                            }
                          });
    alertDialog.show();
  }
}
