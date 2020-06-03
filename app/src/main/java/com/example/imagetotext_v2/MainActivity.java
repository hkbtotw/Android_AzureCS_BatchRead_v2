package com.example.imagetotext_v2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.gson.Gson;
//import com.karumi.dexter.Dexter;
//import com.karumi.dexter.PermissionToken;
//import com.karumi.dexter.listener.PermissionDeniedResponse;
//import com.karumi.dexter.listener.PermissionGrantedResponse;
//import com.karumi.dexter.listener.PermissionRequest;
//import com.karumi.dexter.listener.single.PermissionListener;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edmt.dev.edmtdevcognitivevision.Contract.AnalysisInDomainResult;
import edmt.dev.edmtdevcognitivevision.Contract.AnalysisResult;
import edmt.dev.edmtdevcognitivevision.Contract.HandwritingRecognitionOperation;
import edmt.dev.edmtdevcognitivevision.Contract.HandwritingRecognitionOperationResult;
import edmt.dev.edmtdevcognitivevision.Contract.Model;
import edmt.dev.edmtdevcognitivevision.Contract.ModelResult;
import edmt.dev.edmtdevcognitivevision.Contract.OCR;
import edmt.dev.edmtdevcognitivevision.Rest.VisionServiceException;
import edmt.dev.edmtdevcognitivevision.Rest.WebServiceRequest;
import edmt.dev.edmtdevcognitivevision.Utils.Utils;
import edmt.dev.edmtdevcognitivevision.VisionServiceClient;
import edmt.dev.edmtdevcognitivevision.VisionServiceRestClient;

public class MainActivity extends AppCompatActivity {

    Button mLoadImage;
    ImageView mPreviewIv;
    ImageView mProcessedImageView;
    TextView mResultView;
    RadioButton mDigital;
    RadioButton mAnalog;
    RadioGroup mMeterSelection;
    EditText mEditResult;
    Button mRecordData;
    RadioGroup mTripSelection;
    EditText mTripId;
    EditText mUserId;
    Button mLoadReport;
    Button mLocOn;
    Button mLocOff;
    TextView mDistanceView;

    TextView txt_location;

    private static final int TIME = 5000;
    private static final int FASTTIME = 3000;
    private static final int DISTANCE = 5;  // Check at every i second  (i *1000) if the location changed more than distance, to update location
    private LocationManager lcm;
    private ArrayList latLonList;
    public Boolean recordFlag;
    public LatLng currentPosition;
    public LatLng previousPosition;
    public Location lastLocation=null;
    private double lastDistance=0;
    public double sumDistance=0;
    public Bitmap bitmapBck;

    private static final int sharpenWeight = 20;

    private BroadcastReceiver broadcastReceiver;

    private static final int CAMERA_REQUEST_CODE=200;
    private static final int STORAGE_REQUEST_CODE=400;
    private static final int IMAGE_PICK_GALLERY_CODE=1000;
    private static final int IMAGE_PICK_CAMERA_CODE=1001;

    private final String API_KEY="";
    private final String API_LINK="https://southeastasia.api.cognitive.microsoft.com/vision/v2.0";
    VisionServiceClient visionServiceClient=new VisionServiceRestClient(API_KEY,API_LINK);
    VisionBatchRead visionBatchRead=new VisionBatchRead(API_KEY,API_LINK);
    ImageProcessing imageProcessing=new ImageProcessing();
    ParseServer parseServer=new ParseServer();

    String cameraPermission[];
    String storagePermission[];
    Uri image_uri;
    public boolean flagMeter;
    public String tripString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mLoadImage=findViewById(R.id.loadImage);
        mPreviewIv=findViewById(R.id.imageView);
        mProcessedImageView=findViewById(R.id.processedImageView);
        mResultView=findViewById(R.id.resultView);
        mDigital=findViewById(R.id.digitalMeter);
        mAnalog=findViewById(R.id.analogMeter);
        mMeterSelection=findViewById(R.id.meterSelection);
        mEditResult=findViewById(R.id.resultEdit);
        mRecordData=findViewById(R.id.recordData);
        mTripSelection=findViewById(R.id.tripSelection);
        mTripId=findViewById(R.id.tripId);
        mUserId=findViewById(R.id.userId);
        mLoadReport=findViewById(R.id.loadReport);
        mLocOn=findViewById(R.id.onLocation);
        mLocOff=findViewById(R.id.offLocation);
        mDistanceView=findViewById(R.id.distanceView);

        txt_location=findViewById(R.id.txt_location);


        if(runtime_permission()){
            Log.i("Warning","Need permission !!!!");
        }// runtime

        cameraPermission=new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        recordFlag=false;
        lcm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        latLonList= new ArrayList<String>();

        final boolean gpsEnabled = lcm.isProviderEnabled(
                LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            new EnableGpsDialogFragment().show(getSupportFragmentManager(), "enableGpsDialog");
        } // if

        mLocOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this, LocationService.class);
                startService(intent);

                /*
                recordFlag=true;
                Toast.makeText(MainActivity.this,"Start Recording Running Distance ",Toast.LENGTH_LONG).show();
                */

            }
        }); //mLocOn

        mLocOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"Recording covered distances to Server", Toast.LENGTH_LONG).show();
                Intent intent=new Intent(MainActivity.this, LocationService.class);
                stopService(intent);

                recordFlag=true;



            }
        }); //mLocOff

        mLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedid=mMeterSelection.getCheckedRadioButtonId();
                Log.i("selectId"," choice : "+selectedid);
                switch (selectedid){
                    case R.id.analogMeter:
                        flagMeter=true;
                        Log.i("selectId"," meter  :  1 "+selectedid+" "+flagMeter);
                        break;

                    case R.id.digitalMeter:
                        flagMeter=false;
                        Log.i("selectId"," meter  :  2 "+selectedid+" "+flagMeter);
                        break;
                }
                Log.i("LoadImg"," Checked --  "+flagMeter);
                showImageImportDialog();
            } // onClick

        }); //setOnClickListener

        mRecordData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedid=mTripSelection.getCheckedRadioButtonId();
                Log.i("selectId"," choice : "+selectedid);
                switch (selectedid){
                    case R.id.goTrip:
                        tripString="GO";
                        Log.i("selectId"," meter  :  1 "+selectedid+" "+tripString);
                        break;

                    case R.id.backTrip:
                        tripString="BACK";
                        Log.i("selectId"," meter  :  2 "+selectedid+" "+tripString);
                        break;
                }
                Log.i("LoadImg"," Checked --  "+flagMeter+"  : "+tripString);

                String tripIdIn=mTripId.getText().toString();
                mResultView.setText("Mile number : "+mEditResult.getText()+" in "+tripString+" of trip no: "+tripIdIn);

                //ref: https://stackoverflow.com/questions/1119583/how-do-i-show-the-number-keyboard-on-an-edittext-in-android
                // Show only number keys for text editing
                mEditResult.setInputType(InputType.TYPE_CLASS_NUMBER);
                KeyListener keyListener = DigitsKeyListener.getInstance("1234567890");
                mEditResult.setKeyListener(keyListener);

                String test=mEditResult.getText().toString();

                //ref: https://stackoverflow.com/questions/10372862/java-string-remove-all-non-numeric-characters
                // replace all non numeric characters by blank
                String digitText = test.replaceAll("[^0-9.]", "");
                Log.i("GETTEXT","test : "+digitText);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Recording .... ");
                builder.setMessage("Would you like to submit data?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Toast.makeText(MainActivity.this," Recording to Parse Server ",Toast.LENGTH_SHORT).show();

                        SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-YYYY");

                        SimpleDateFormat sdfFull=new SimpleDateFormat("dd-MM-YYYY HH:MM:ss");
                        String currentDatetime=sdf.format(new Date());
                        String currentDatetimeFull=sdfFull.format(new Date());


                        // Save data to Parse server
                        ParseObject object = new ParseObject("DriverMonitor");
                        String refCol=mUserId.getText().toString()+"-"+mTripId.getText().toString()+"-"+currentDatetime;
                        if(tripString=="GO"){
                            Log.i("parse"," tripString : "+tripString+" : "+refCol);
                            parseServer.SaveToParseServerGO(object,mEditResult.getText().toString(),mTripId.getText().toString(),currentDatetimeFull,mUserId.getText().toString(),refCol);
                        }else{
                            Log.i("parse"," tripString : "+tripString+" : "+refCol);
                            parseServer.SaveToParseServerBACK(object, refCol, mEditResult.getText().toString(), currentDatetimeFull);
                        }



                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // Stuff to do
                        dialog.dismiss();
                    }
                });

                AlertDialog d = builder.create();
                d.show();

            }  // onClick
        }); // mRecordData.setOnClickListener

        mLoadReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"Loading report",Toast.LENGTH_SHORT).show();
                SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-YYYY");
                String currentDatetime=sdf.format(new Date());
                String refCol=mUserId.getText().toString()+"-"+mTripId.getText().toString()+"-"+currentDatetime;
                try {
                    Thread.sleep(2000);
                    parseServer.ReadFromParseServer(refCol);
                    Thread.sleep(2000);
                    parseServer.ReadFromParseServer(refCol);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent intent=new Intent(getApplicationContext(), DriverReport.class);
                intent.putExtra("username",mUserId.getText().toString());
                intent.putExtra("tripId",mTripId.getText().toString());
                intent.putExtra("sumDistance",sumDistance);
                startActivity(intent);
            }
        }); // mLoadReport

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

    } // On Create


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    } // isMyServiceRunning


    private boolean runtime_permission() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return true;
        }
        return false;
    } // runtime_permission


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver!=null){
            unregisterReceiver(broadcastReceiver);
        }
    }// onDestroy

    @Override
    protected void onResume() {
        super.onResume();
        LocationService mSensorService = new LocationService(getApplicationContext());
        Intent mServiceIntent = new Intent(getApplicationContext(), mSensorService.getClass());
        if (!isMyServiceRunning(mSensorService.getClass())) {
            startService(mServiceIntent);
        }
        if(broadcastReceiver == null){
            broadcastReceiver=new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    double lat=intent.getExtras().getDouble("Lat");
                    double lng=intent.getExtras().getDouble("Lng");
                    double sumDistance=intent.getExtras().getDouble("distance");
                    //txt_location.setText(" Lat : "+lat+", Lng : "+lng+ " --> "+sumDistance);
                    DecimalFormat df = new DecimalFormat("#.#");
                    String dummy = df.format(sumDistance);

                    Log.i("text"," text out : "+dummy);
                    mDistanceView.setText("Running : "+dummy+" km");

                    if(recordFlag=true){
                        recordFlag=false;
                        ParseObject object = new ParseObject("DriverMonitor");
                        SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-YYYY");
                        String currentDatetime=sdf.format(new Date());


                        Log.i("checkDist","sumDistance : "+sumDistance);
                        // Save data to Parse server
                        object = new ParseObject("DriverMonitor");
                        String refCol=mUserId.getText().toString()+"-"+mTripId.getText().toString()+"-"+currentDatetime;
                        parseServer.SaveToParseServerLATLNG(object,refCol, (float) sumDistance);
                        Toast.makeText(MainActivity.this,"Covered Distance Recorded : "+sumDistance,Toast.LENGTH_LONG).show();
                    }


                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));


        /*
        //lcm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);

            return;
        }

        Criteria criteria=new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        String provider = lcm.getBestProvider(criteria, true);

        Log.d("Location"," best provider "+ provider);

        //lcm.requestLocationUpdates(LocationManager.GPS_PROVIDER,TIME, DISTANCE, listener);

        lcm.requestLocationUpdates(provider,
                TIME, DISTANCE, listener);
        */

    } // onResume

    /*
    private final LocationListener listener= new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentPosition= new LatLng(location.getLatitude(),location.getLongitude());
            //if(previousPosition!=null) {
            //    Log.d("Position", " Current " + currentPosition + ", Previous " + previousPosition);
            //}


            //Log.d("Position", " Current "+currentPosition+", Previous "+previousPosition);
            if(recordFlag){
                previousPosition=currentPosition;
                updateWithNewLocation(location);
                Log.d("Flag", "(" + location.getLatitude() + "," + location.getLongitude() + ")");}else{
                Log.d("Flag", " Flag "+recordFlag);
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }; // End LocationListener

    @Override
    public void onStop(){
        super.onStop();
        lcm.removeUpdates(listener);
    } // onStop

    private void updateWithNewLocation(Location location){
        String latLongString="";
        if(location!=null){
            double lat=location.getLatitude();
            double lng=location.getLongitude();
            latLongString="Lat: "+lat+" ::  Long : "+lng;

            if(lastLocation!=null){
                double elaspedTime=(location.getTime()-lastLocation.getTime())/1000;
                lastDistance=lastLocation.distanceTo(location);
                sumDistance=sumDistance+(lastDistance/1000);
            }
            this.lastLocation=location;
            Log.d("Speed"," distance "+lastDistance+" sumDist "+sumDistance);


        } else {
            latLongString= " No location found ";
        } // if

        SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-YYYY HH:mm:ss");
        String currentDatetime=sdf.format(new Date());
        String out=" [ " +currentDatetime+ ","+ location.getLatitude() + "," + location.getLongitude() + " ] ";
        latLonList.add(out);

        Log.d("List Out", latLonList.toString() );
        Log.d("DateTime","location date :" + sdf.format(location.getTime()));

        //ParseObject object = new ParseObject("DriverMonitor");
        //sdf=new SimpleDateFormat("dd-MM-YYYY");
        //currentDatetime=sdf.format(new Date());

        // Save data to Parse server
        //object = new ParseObject("DriverMonitor");
        //String refCol=mUserId.getText().toString()+"-"+mTripId.getText().toString()+"-"+currentDatetime;
        //parseServer.SaveToParseServerLATLNG(object,refCol, (float) sumDistance);

        DecimalFormat df = new DecimalFormat("#.#");
        String dummy = df.format(sumDistance);

        Log.i("text"," text out : "+dummy);
        mDistanceView.setText("Running : "+dummy+" km");
        //WriteFile(latLongString);

    } //updateWithNewLocation
    */


    private void showImageImportDialog() {
        String[] items={" Camera", "Gallery"};
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==0){
                    Log.i("Select","Pick Camera");
                    if(!checkCameraPermission()){
                        requestCameraPermission();
                    }else{
                        pickCamera();
                    }
                }
                if(which==1){
                    Log.i("Select","Pieck Gallery");
                    if(!checkStoragePermission()){
                        requestStoragePermission();
                    }else{
                        pickGallery();
                    }


                }
            }
        });
        dialog.create().show();
    }

    private void pickGallery() {
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);

    }

    private void pickCamera() {
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"NewPic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Image To Text");
        image_uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);

    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);

    }

    private boolean checkStoragePermission() {
        boolean result= ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result;

    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result= ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean result1=ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case CAMERA_REQUEST_CODE:
                if(grantResults.length>0){
                    boolean cameraAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && writeStorageAccepted){
                        pickCamera();
                    }else{
                        Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                    }

                }
                break;

            case STORAGE_REQUEST_CODE:
                if(grantResults.length>0){
                    boolean writeStorageAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    if(writeStorageAccepted){
                        pickGallery();
                    }else{
                        Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                    }

                }
                break;

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                CropImage.activity(data.getData()).setGuidelines(CropImageView.Guidelines.ON).start(this);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                CropImage.activity(image_uri).setGuidelines(CropImageView.Guidelines.ON).start(this);
            }
        }
        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result=CropImage.getActivityResult(data);
            if(resultCode==RESULT_OK){
                Uri resultUri=result.getUri();
                mPreviewIv.setImageURI(resultUri);
                BitmapDrawable bitmapDrawable=(BitmapDrawable)mPreviewIv.getDrawable();
                Bitmap bitmap=bitmapDrawable.getBitmap();
                bitmapBck=bitmap;

                // Action start HERE
                Bitmap bitmap1=bitmap;
                Log.i("select","flagMeter : "+flagMeter);
                if(!flagMeter){
                    // toBinary ref: https://stackoverflow.com/questions/20299264/android-convert-grayscale-to-binary-image
                    // and https://stackoverflow.com/questions/6764839/error-with-setpixels
                    Bitmap bitDummy2=imageProcessing.toBinary(bitmap);
                    Bitmap bitDummy=imageProcessing.RemoveNoise(bitDummy2);
                    bitmap1=imageProcessing.sharpen(bitDummy,sharpenWeight);

                }else{
                    Bitmap bitDummy=imageProcessing.toGrayscale(bitmap);
                    //Bitmap bitDummy=imageProcessing.RemoveNoise(bitDummy2);
                    bitmap1=imageProcessing.sharpen(bitDummy,sharpenWeight);
                }
                mPreviewIv.setImageBitmap(bitmap1);
                int height = bitmap1.getHeight();
                int width = bitmap1.getWidth();
                Bitmap newBitmap=Bitmap.createScaledBitmap(bitmap1,width*2,height*2,true);

                // View processed image for checking
                //mProcessedImageView.setImageBitmap(newBitmap);


                ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
                //bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                newBitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                final ByteArrayInputStream inputStream=new ByteArrayInputStream(outputStream.toByteArray());

                AsyncTask<InputStream,String, String> visionTask=new AsyncTask<InputStream, String, String>() {
                    ProgressDialog progressDialog=new ProgressDialog(MainActivity.this);

                    @Override
                    protected void onPreExecute(){
                        progressDialog.show();
                    }

                    @Override
                    protected String doInBackground(InputStream... inputStreams) {
                        try{
                            publishProgress("Recognizing...");
                            //String[] features={"Description"};
                            String[] features={};
                            String[] details={};
                            StringBuilder result=visionBatchRead.analyzeImage1(inputStreams[0],features,details);
                            Log.i("Result"," textview : "+result);
                            Log.i("Result"," textview : "+result.toString());

                            String resultOut=result.toString();
                            if(resultOut.length()==0){
                                resultOut="Not Readble";
                            }
                            //AnalysisResult result=visionServiceClient.analyzeImage(inputStreams[0],features,details);
                            //OCR result=visionServiceClient.recognizeText(inputStreams[0],"en",true);
                            //String jsonResult=new Gson().toJson(result);
                            //Log.i("info","-->"+jsonResult);
                            return resultOut;

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (VisionServiceException e) {
                            e.printStackTrace();
                        }
                        return "";
                    }

                    @Override
                    protected void onPostExecute(String s){
                        if(TextUtils.isEmpty(s)){
                            Toast.makeText(MainActivity.this,"API return empty",Toast.LENGTH_SHORT).show();

                        }else {
                            progressDialog.dismiss();


                            /*
                            OCR result = new Gson().fromJson(s, OCR.class);
                            StringBuilder result_text = new StringBuilder();

                            Log.i("OCR"," ==> "+s);
                            int regionNumber= result.regions.size();
                            int lineNumer=result.regions.get(0).lines.size();
                            int wordNumber;

                            //Log.i("OCR"," ==> "+ result.regions.get(0).lines.get(0).words.size());
                            Log.i("OCR"," RegionNumber ==> "+ regionNumber);
                            Log.i("OCR"," LineNumber ==> "+ lineNumer);
                            for(int i=0; i<lineNumer; i++){

                                wordNumber=result.regions.get(0).lines.get(i).words.size();
                                Log.i("OCR"," wordNumber ==> "+ wordNumber+" "+i);
                                for(int j=0; j<wordNumber;j++){
                                    Log.i("OCR"," ==> "+ result.regions.get(0).lines.get(i).words.get(j).text+" "+j);
                                    result_text.append(result.regions.get(0).lines.get(i).words.get(j).text+" ");
                                }
                            }
                            txtResult.setText(result_text.toString());  */
                            Log.i("Result"," textview : "+s);
                            //mResultView.setText(s);
                            //mEditResult.setText(mResultView.getText());
                            String digitText1 = s.replaceAll("[^0-9.]", "");
                            String digitText = digitText1.replaceAll("[^a-zA-Z0-9]", "");
                            String subDigit;
                            if(digitText.length()>=6){
                                subDigit=digitText.substring(0,6);
                            }else{
                                subDigit=digitText;
                            }
                            if(subDigit.length()==6 ){
                                mEditResult.setText(subDigit);
                            }else{
                                mEditResult.setText(": Not readable");
                                if(!flagMeter || subDigit.length()<6){
                                    RepeatedRecognition();
                                }//repeated
                            }


                        }
                    }

                    @Override
                    protected void onProgressUpdate(String... values){
                        progressDialog.setMessage(values[0]);
                    }
                };  //AsyncTask

                visionTask.execute(inputStream);

                // Action Ends HERE

            }else if(resultCode==CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Exception error=result.getError();
                Toast.makeText(this,""+error,Toast.LENGTH_SHORT ).show();
            }
        }
    }  // On Activity Result


    public void RepeatedRecognition(){
        Log.i("repeat","Get in REPEAT");


        // Action start HERE
        Bitmap bitmap1;
        Log.i("repeat","flagMeter : "+flagMeter);

        Bitmap bitDummy=imageProcessing.toGrayscale(bitmapBck);
        //Bitmap bitDummy=imageProcessing.RemoveNoise(bitDummy2);
        bitmap1=imageProcessing.sharpen(bitDummy,sharpenWeight);
        //bitmap1=bitmapBck;

        mPreviewIv.setImageBitmap(bitmap1);
        //bitmap1=bitmap;


        int height = bitmap1.getHeight();
        int width = bitmap1.getWidth();
        Bitmap newBitmap=Bitmap.createScaledBitmap(bitmap1,width*1,height*1,true);

        // View processed image for checking
        //mProcessedImageView.setImageBitmap(newBitmap);
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        //bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        newBitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        final ByteArrayInputStream inputStream=new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream,String, String> visionTask=new AsyncTask<InputStream, String, String>() {
            ProgressDialog progressDialog=new ProgressDialog(MainActivity.this);

            @Override
            protected void onPreExecute(){
                progressDialog.show();
            }

            @Override
            protected String doInBackground(InputStream... inputStreams) {
                try{
                    publishProgress("Recognizing...");
                    //String[] features={"Description"};
                    String[] features={};
                    String[] details={};
                    StringBuilder result=visionBatchRead.analyzeImage1(inputStreams[0],features,details);
                    Log.i("Result"," textview : "+result);
                    Log.i("Result"," textview : "+result.toString());

                    String resultOut=result.toString();
                    if(resultOut.length()==0){
                        resultOut="Not Readble";
                    }
                    //AnalysisResult result=visionServiceClient.analyzeImage(inputStreams[0],features,details);
                    //OCR result=visionServiceClient.recognizeText(inputStreams[0],"en",true);
                    //String jsonResult=new Gson().toJson(result);
                    //Log.i("info","-->"+jsonResult);
                    return resultOut;

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (VisionServiceException e) {
                    e.printStackTrace();
                }
                return "";
            }

            @Override
            protected void onPostExecute(String s){
                if(TextUtils.isEmpty(s)){
                    Toast.makeText(MainActivity.this,"API return empty",Toast.LENGTH_SHORT).show();

                }else {
                    progressDialog.dismiss();


                            /*
                            OCR result = new Gson().fromJson(s, OCR.class);
                            StringBuilder result_text = new StringBuilder();

                            Log.i("OCR"," ==> "+s);
                            int regionNumber= result.regions.size();
                            int lineNumer=result.regions.get(0).lines.size();
                            int wordNumber;

                            //Log.i("OCR"," ==> "+ result.regions.get(0).lines.get(0).words.size());
                            Log.i("OCR"," RegionNumber ==> "+ regionNumber);
                            Log.i("OCR"," LineNumber ==> "+ lineNumer);
                            for(int i=0; i<lineNumer; i++){

                                wordNumber=result.regions.get(0).lines.get(i).words.size();
                                Log.i("OCR"," wordNumber ==> "+ wordNumber+" "+i);
                                for(int j=0; j<wordNumber;j++){
                                    Log.i("OCR"," ==> "+ result.regions.get(0).lines.get(i).words.get(j).text+" "+j);
                                    result_text.append(result.regions.get(0).lines.get(i).words.get(j).text+" ");
                                }
                            }
                            txtResult.setText(result_text.toString());  */
                    Log.i("Result"," textview : "+s);
                    //mResultView.setText(s);
                    //mEditResult.setText(mResultView.getText());
                    String digitText1 = s.replaceAll("[^0-9.]", "");
                    String digitText = digitText1.replaceAll("[^a-zA-Z0-9]", "");
                    String subDigit;
                    if(digitText.length()>=6){
                        subDigit=digitText.substring(0,6);
                    }else{
                        subDigit=digitText;
                    }
                    if(subDigit.length()==6){
                        mEditResult.setText(subDigit);
                    }else{

                        //mEditResult.setText("; Not readable");
                        GoogleTextRecognition();
                    }


                }
            }

            @Override
            protected void onProgressUpdate(String... values){
                progressDialog.setMessage(values[0]);
            }
        };  //AsyncTask

        visionTask.execute(inputStream);


    }//RepeatedRecognition

    public void GoogleTextRecognition(){
        Toast.makeText(MainActivity.this, "Google Recognition Running", Toast.LENGTH_SHORT).show();
        Bitmap bitmap=bitmapBck;
        mPreviewIv.setImageBitmap(bitmap);

        TextRecognizer recognizer=new TextRecognizer.Builder(getApplicationContext()).build();

        if(!recognizer.isOperational()){
            Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show();

        }else {
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> items = recognizer.detect(frame);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                TextBlock myItem = items.valueAt(i);
                sb.append(myItem.getValue());
            }

            Log.i("GoogleR"," See : "+sb.toString());
            String digitText1 = sb.toString().replaceAll("[^0-9.]", "");
            String digitText = digitText1.replaceAll("[^a-zA-Z0-9]", "");
            String subDigit;
            if(digitText.length()>=6){
                subDigit=digitText.substring(0,6);
            }else{
                subDigit=digitText;
            }

            mEditResult.setText(subDigit);
        }

    }//GoogleTextRecognition

    public static class EnableGpsDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            return new android.app.AlertDialog.Builder(getActivity())
                    .setTitle("GPS System")
                    .setMessage(" Enable GPS to use Tracker ")
                    .setPositiveButton("Setting ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent settingsIntent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(settingsIntent);
                        }
                    })
                    .create();
        } //onCreateDialog

    } // EnableGpsDialogFragment
} // Program


class ParseServer{
    public void SaveToParseServerGO(ParseObject object,String mileIn, String tripIdIn, String dateIn,String userIn, String refColIn){
        object.put("tripId", tripIdIn);
        object.put("goMile", mileIn);
        object.put("goDate", dateIn);
        object.put("backMile", "");
        object.put("backDate", "");
        object.put("calTotTrip", 0);
        object.put("MeasuredTotTrip", 0);
        object.put("user",userIn);
        object.put("refCol",refColIn);

        object.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException ex) {
                if (ex == null) {
                    Log.i("Parse Result", "Successful!");
                } else {
                    Log.i("Parse Result", "Failed  GO " + ex.toString());
                }
            }
        });
        ParseUser.enableAutomaticUser();

        ParseACL defaultACL = new ParseACL();
        defaultACL.setPublicReadAccess(true);
        defaultACL.setPublicWriteAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);
    }// SaveTo ParseServer GO

    public void SaveToParseServerBACK(final ParseObject object, String refCol, final String mileIn, final String dateIn){
        ParseQuery<ParseObject> query=new ParseQuery<ParseObject>("DriverMonitor");
        query.whereEqualTo("refCol",refCol);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null && objects.size()>0){
                    objects.get(0).put("backMile", mileIn);
                    objects.get(0).put("backDate", dateIn);
                    objects.get(0).saveInBackground();
                    Log.i("test", " Written successfully. ");
                }else{
                    Log.i("test", " Null ");
                    e.printStackTrace();
                }
            }
        });

    } //SaveTo ParseServer BACK

    public void SaveToParseServerLATLNG(final ParseObject object, String refCol, final float distIn){
        ParseQuery<ParseObject> query=new ParseQuery<ParseObject>("DriverMonitor");
        query.whereEqualTo("refCol",refCol);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null && objects.size()>0){
                    objects.get(0).put("MeasuredTotTrip", distIn);
                    objects.get(0).saveInBackground();
                    Log.i("test", " Written successfully. ");
                }else{
                    Log.i("test", " Null ");
                    e.printStackTrace();
                }
            }
        });

    }// SaveTo ParseServer LATLNG

    public void ReadFromParseServer(String refCol){
        ParseQuery<ParseObject> query=new ParseQuery<ParseObject>("DriverMonitor");
        query.whereEqualTo("refCol",refCol);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0) {
                    String goMileStr = objects.get(0).getString("goMile");
                    float goNumber = Float.parseFloat(goMileStr);
                    String backMileStr = objects.get(0).getString("backMile");
                    float backNumber = Float.parseFloat(backMileStr);
                    float coverage = backNumber - goNumber;
                    Log.i("coverage", " Distance covered " + coverage);
                    objects.get(0).put("calTotTrip", coverage);
                    objects.get(0).saveInBackground();
                    Log.i("coverage", " Distance covered " + goMileStr+" : "+backMileStr);
                } else {
                    Log.i("coverage", " Null ");
                    e.printStackTrace();
                }

            }  // done

            }); // query
    }// ReadFromParseServer

} // ParseServer

class ImageProcessing{
    public static Bitmap convertImage(Bitmap original){
        Bitmap finalImage = Bitmap.createBitmap(original.getWidth(), original.getHeight(), original.getConfig());
        int A, R, G, B;
        int colorPixel;
        int width=original.getWidth();
        int height=original.getHeight();

        for(int x=0; x<width; x++){
            for(int y=0;y<height;y++){
                colorPixel=original.getPixel(x,y);
                A= Color.alpha(colorPixel);
                R= Color.red(colorPixel);
                G= Color.green(colorPixel);
                B= Color.blue(colorPixel);

                R=(R+G+B)/3;
                G=R;
                B=R;

                finalImage.setPixel(x,y,Color.argb(A,R,G,B));
            } // for y
        }// for
        return finalImage;
    } // Bitmap

    public Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }  // tograyscale


    public Bitmap toBinary(Bitmap bmpOriginal) {
        int width, height, threshold;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        threshold = 127;

        Bitmap bmpBinary = Bitmap.createBitmap(bmpOriginal);
        bmpBinary = bmpBinary.copy( Bitmap.Config.ARGB_8888 , true);

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                // get one pixel color
                int pixel = bmpOriginal.getPixel(x, y);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                int gray = (int)(red * 0.3 + green * 0.59 + blue * 0.11);

                //get binary value
                if(gray < threshold){
                //if(red < threshold){
                    bmpBinary.setPixel(x, y, 0xFF000000);
                    //Log.i("color"," pixel : 1"+x+" "+y);
                } else{
                    bmpBinary.setPixel(x, y, 0xFFFFFFFF);
                    //Log.i("color"," pixel : 2"+x+" "+y);
                }

            }
        }
        return bmpBinary;
    } // toBinary

    //ref : https://xjaphx.wordpress.com/2011/06/22/image-processing-convolution-matrix/
    // filter and convolution class
    public Bitmap sharpen(Bitmap src, double weight) {
        double[][] SharpConfig = new double[][] {
                { 0 , -2    , 0  },
                { -2, weight, -2 },
                { 0 , -2    , 0  }
        };
        ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
        convMatrix.applyConfig(SharpConfig);
        convMatrix.Factor = weight - 8;
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
    }//sharpen filter

    // ref: https://stackoverflow.com/questions/32077223/android-image-noise-removal
    // Noise remover
    public Bitmap RemoveNoise(Bitmap bmap) {
        for (int x = 0; x < bmap.getWidth(); x++) {
            for (int y = 0; y < bmap.getHeight(); y++) {
                int pixel = bmap.getPixel(x, y);
                int R = Color.red(pixel);
                int G = Color.green(pixel);
                int B = Color.blue(pixel);
                if (R < 162 && G < 162 && B < 162)
                    bmap.setPixel(x, y, Color.BLACK);
            }
        }
        for (int  x = 0; x < bmap.getWidth(); x++) {
            for (int y = 0; y < bmap.getHeight(); y++) {
                int pixel = bmap.getPixel(x, y);
                int R = Color.red(pixel);
                int G = Color.green(pixel);
                int B = Color.blue(pixel);
                if (R > 162 && G > 162 && B > 162)
                    bmap.setPixel(x, y, Color.WHITE);
            }
        }
        return bmap;
    }// RemoveNoise

}// class ImageProcessing

class ConvolutionMatrix{
    public static final int SIZE = 3;

    public double[][] Matrix;
    public double Factor = 1;
    public double Offset = 1;

    public ConvolutionMatrix(int size) {
        Matrix = new double[size][size];
    }

    public void setAll(double value) {
        for (int x = 0; x < SIZE; ++x) {
            for (int y = 0; y < SIZE; ++y) {
                Matrix[x][y] = value;
            }
        }
    }

    public void applyConfig(double[][] config) {
        for(int x = 0; x < SIZE; ++x) {
            for(int y = 0; y < SIZE; ++y) {
                Matrix[x][y] = config[x][y];
            }
        }
    }

    public static Bitmap computeConvolution3x3(Bitmap src, ConvolutionMatrix matrix) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, src.getConfig());

        int A, R, G, B;
        int sumR, sumG, sumB;
        int[][] pixels = new int[SIZE][SIZE];

        for(int y = 0; y < height - 2; ++y) {
            for(int x = 0; x < width - 2; ++x) {

                // get pixel matrix
                for(int i = 0; i < SIZE; ++i) {
                    for(int j = 0; j < SIZE; ++j) {
                        pixels[i][j] = src.getPixel(x + i, y + j);
                    }
                }

                // get alpha of center pixel
                A = Color.alpha(pixels[1][1]);

                // init color sum
                sumR = sumG = sumB = 0;

                // get sum of RGB on matrix
                for(int i = 0; i < SIZE; ++i) {
                    for(int j = 0; j < SIZE; ++j) {
                        sumR += (Color.red(pixels[i][j]) * matrix.Matrix[i][j]);
                        sumG += (Color.green(pixels[i][j]) * matrix.Matrix[i][j]);
                        sumB += (Color.blue(pixels[i][j]) * matrix.Matrix[i][j]);
                    }
                }

                // get final Red
                R = (int)(sumR / matrix.Factor + matrix.Offset);
                if(R < 0) { R = 0; }
                else if(R > 255) { R = 255; }

                // get final Green
                G = (int)(sumG / matrix.Factor + matrix.Offset);
                if(G < 0) { G = 0; }
                else if(G > 255) { G = 255; }

                // get final Blue
                B = (int)(sumB / matrix.Factor + matrix.Offset);
                if(B < 0) { B = 0; }
                else if(B > 255) { B = 255; }

                // apply new pixel
                result.setPixel(x + 1, y + 1, Color.argb(A, R, G, B));
            }
        }

        // final image
        return result;
    }
}// class convolution matrix

class VisionBatchRead implements VisionServiceClient {
    private Gson gson = new Gson();
    private final WebServiceRequest restCall;
    private final String apiRoot;
    private final String API_LINK="https://southeastasia.api.cognitive.microsoft.com/vision/v2.0";

    public VisionBatchRead(String subscriptKey, String apiRoot) {
        this.restCall = new WebServiceRequest(subscriptKey);
        this.apiRoot = apiRoot.replaceAll("/$", "");
    }

    @Override
    public AnalysisResult analyzeImage(String s, String[] strings, String[] strings1) throws VisionServiceException {
        return null;
    }

    @Override
    public AnalysisResult analyzeImage(InputStream inputStream, String[] strings, String[] strings1) throws VisionServiceException, IOException {
        return null;
    }


    public StringBuilder analyzeImage1(InputStream stream, String[] visualFeatures, String[] details) throws VisionServiceException, IOException {
        Map<String, Object> params = new HashMap<>();
        AppendParams(params, "visualFeatures", visualFeatures);
        AppendParams(params, "details", details);
        String path = apiRoot + "/read/core/asyncBatchAnalyze";
        String uri = WebServiceRequest.getUrl(path, params);

        params.clear();
        byte[] data = Utils.toByteArray(stream);
        params.put("data", data);
        Log.i("uri"," uri : "+uri);
        String json_uri = (String) this.restCall.request(uri, "POST", params, "application/octet-stream", false);
        //Log.i("json"," string json + "+json_uri);
        String json = (String) this.restCall.request(json_uri, "GET", params, "application/octet-stream", false);
        //Log.i("json"," string json + "+json);
        BatchReadResult out = this.gson.fromJson(json, BatchReadResult.class);
        while(out.status.equals("Running")){
            json = (String) this.restCall.request(json_uri, "GET", params, "application/octet-stream", false);
            //Log.i("json"," string json Loop + "+json);
            out = this.gson.fromJson(json, BatchReadResult.class);
            Log.i("out"," string json Loop + "+out.status);
        }

        /*try {
            Thread.sleep(3000);
            json = (String) this.restCall.request(json_uri, "GET", params, "application/octet-stream", false);
            Log.i("json"," string json + "+json);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        //BatchReadResult out = this.gson.fromJson(json, BatchReadResult.class);

        Log.i("out"," GO string json + "+json);

        //Log.i("out"," GO string json + "+out.recognitionResults.get(0).lines.get(0));
        StringBuilder result_text = new StringBuilder();
        try {
            JSONObject reader=new JSONObject(json);
            JSONArray sys=reader.getJSONArray("recognitionResults");
            int jsonLen=sys.length();
            for(int i=0; i<jsonLen ;i++) {
                JSONObject lines = sys.getJSONObject(i);
                Log.i("out", " GO pixel + " + lines);
                JSONArray lines1=lines.getJSONArray("lines");
                Log.i("out", " GO pixel +1 " + lines1);
                int lineLen=lines1.length();
                for(int j=0;j<lineLen;j++) {
                    JSONObject word=lines1.getJSONObject(j);
                    JSONArray word1=word.getJSONArray("words");
                    Log.i("out", " GO pixel + " + word1);
                    int wordLen=word1.length();
                    for(int k=0;k<wordLen;k++) {
                        JSONObject text=word1.getJSONObject(k);
                        String text1=text.getString("text");
                        Log.i("out", " GO pixel +text " + text1);
                        result_text.append(text1);

                    }

                }
                /*
                JSONObject words=lines.getJSONObject("words");
                int wordLen=words.length();
                for(int j=0;j<wordLen;j++) {
                    String textin=words.getString("text");
                    Log.i("out", " GO pixel + " + textin);
                }
                */
            }
            Log.i("out"," GO text + "+result_text);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //AnalysisResult visualFeature = this.gson.fromJson(json, AnalysisResult.class);
        return result_text;
    }

    @Override
    public AnalysisInDomainResult analyzeImageInDomain(String s, Model model) throws VisionServiceException {
        return null;
    }

    @Override
    public AnalysisInDomainResult analyzeImageInDomain(String s, String s1) throws VisionServiceException {
        return null;
    }

    @Override
    public AnalysisInDomainResult analyzeImageInDomain(InputStream inputStream, Model model) throws VisionServiceException, IOException {
        return null;
    }

    @Override
    public AnalysisInDomainResult analyzeImageInDomain(InputStream inputStream, String s) throws VisionServiceException, IOException {
        return null;
    }

    @Override
    public AnalysisResult describe(String s, int i) throws VisionServiceException {
        return null;
    }

    @Override
    public AnalysisResult describe(InputStream inputStream, int i) throws VisionServiceException, IOException {
        return null;
    }

    @Override
    public ModelResult listModels() throws VisionServiceException {
        return null;
    }

    @Override
    public OCR recognizeText(String s, String s1, boolean b) throws VisionServiceException {
        return null;
    }

    @Override
    public OCR recognizeText(InputStream inputStream, String s, boolean b) throws VisionServiceException, IOException {
        return null;
    }

    @Override
    public HandwritingRecognitionOperation createHandwritingRecognitionOperationAsync(String s) throws VisionServiceException {
        return null;
    }

    @Override
    public HandwritingRecognitionOperation createHandwritingRecognitionOperationAsync(InputStream inputStream) throws VisionServiceException, IOException {
        return null;
    }

    @Override
    public HandwritingRecognitionOperationResult getHandwritingRecognitionOperationResultAsync(String s) throws VisionServiceException {
        return null;
    }

    @Override
    public byte[] getThumbnail(int i, int i1, boolean b, String s) throws VisionServiceException, IOException {
        return new byte[0];
    }

    @Override
    public byte[] getThumbnail(int i, int i1, boolean b, InputStream inputStream) throws VisionServiceException, IOException {
        return new byte[0];
    }

    private void AppendParams(Map<String, Object> params, String name, String[] args) {
        if(args != null && args.length > 0) {
            String features = StringUtils.join(args, ',');
            params.put(name, features);
        }
    }
} //class VisionBatchRead

class BatchReadResult {
    public String status;
} // Class BatchReadResult