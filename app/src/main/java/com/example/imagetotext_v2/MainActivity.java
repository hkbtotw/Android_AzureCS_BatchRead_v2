package com.example.imagetotext_v2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
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
import java.util.HashMap;
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

    private static final int CAMERA_REQUEST_CODE=200;
    private static final int STORAGE_REQUEST_CODE=400;
    private static final int IMAGE_PICK_GALLERY_CODE=1000;
    private static final int IMAGE_PICK_CAMERA_CODE=1001;

    private final String API_KEY="";
    private final String API_LINK="https://southeastasia.api.cognitive.microsoft.com/vision/v2.0";
    VisionServiceClient visionServiceClient=new VisionServiceRestClient(API_KEY,API_LINK);
    VisionBatchRead visionBatchRead=new VisionBatchRead(API_KEY,API_LINK);
    ImageProcessing imageProcessing=new ImageProcessing();

    String cameraPermission[];
    String storagePermission[];
    Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLoadImage=findViewById(R.id.loadImage);
        mPreviewIv=findViewById(R.id.imageView);
        mProcessedImageView=findViewById(R.id.processedImageView);
        mResultView=findViewById(R.id.resultView);

        cameraPermission=new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        mLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("LoadImg"," Checked 1 ");
                showImageImportDialog();

            } // onClick
        }); //setOnClickListener

    }

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

                // Action start HERE
                Bitmap bitmap1=imageProcessing.toGrayscale(bitmap);
                int height = bitmap1.getHeight();
                int width = bitmap1.getWidth();
                Bitmap newBitmap=Bitmap.createScaledBitmap(bitmap1,width*2,height*2,true);
                mProcessedImageView.setImageBitmap(newBitmap);

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


                            //AnalysisResult result=visionServiceClient.analyzeImage(inputStreams[0],features,details);
                            //OCR result=visionServiceClient.recognizeText(inputStreams[0],"en",true);
                            //String jsonResult=new Gson().toJson(result);
                            //Log.i("info","-->"+jsonResult);
                            return result.toString();

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
                            mResultView.setText(s);
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

} // Program

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
}// class ImageProcessing

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