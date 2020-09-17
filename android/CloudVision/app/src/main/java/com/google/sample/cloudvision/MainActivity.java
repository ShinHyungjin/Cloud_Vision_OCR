/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.cloudvision;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY ="AIzaSyBVVRZs85KEZmyZ4KyXrinNvfroJ8BlQ0A";
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private static HashMap<String, String> nutInfo = new HashMap<>();
    private static AlertDialog.Builder builder;
    private static Uri mImageCaptureUri;
    private static EditText mImageDetails;  // 파싱된 데이터가 나열되는 EditText
    private static ImageView mMainImage;    // 찍은 사진을 보여주는 ImageView
    private static TextView mImageAdvice;
    private static Button allCheck,allClear;
    private static String foodcategory[] = {"식품유형", "식품의 유형", "식품의유형","제품의유형", "제품의 유형", "제품명", "제 품 명", ""};
    private static String nutcopy[] = {"칼슘", "나트륨", "탄수화물", "당류", "식이섬유", "지방", "트랜스지방", "포화지방", "콜레스테롤", "단백질","니아신","레티놀","베타카로틴","비타민A","비타민B","비타민C","비타민E","아연","엽산","인지질","철분","칼륨","회분"};
    private static String nuteng [] = {"CALCIUM", "SODIUM", "CARBOHYDRATE", "SUGARS", "DIETARY FIBER", "FAT", "TRANS FAT", "SATURATED FAT", "CHOLESTEROL", "PROTEIN", "NIACIN", "LETINOL", "BETA CAROTENE", "VITAMIN A", "VITAMIN B", "VITAMIN C", "VITAMIN E", "ZINC", "FOLIC ACID", "PHOSPHOLIPID", "IRON", "POTASSIUM", "RAW SEAFOOD"};
    private static ArrayList<String> nut = new ArrayList<>();   // nutcopy에 저장된 칼슘, 나트륨 등을 복사한다
    private static String parse = "";                           // API로 파싱된 전체 데이터
    private static String parseresult = "";                     // 파싱된 전체 데이터를 기존 복사데이터와 매칭 후 결과를 저장함
    private static ArrayList<ItemData> list = new ArrayList<>();// 결과를 Text가 아닌 ListView로 만들려고 시도했던 리스트
    private static ListView m_oListView;                        // 결과를 Text가 아닌 ListView로 만들려고 시도했던 리스트
    private static ItemData oItem;                              // 결과를 Text가 아닌 ListView로 만들려고 시도했던 리스트
    private static ListAdapter oAdapter = null;                 // 결과를 Text가 아닌 ListView로 만들려고 시도했던 리스트


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for(int i=0; i<nutcopy.length; i++)
            nut.add(nutcopy[i]);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder
                    .setMessage(R.string.dialog_select_prompt)
                    .setPositiveButton(R.string.dialog_select_gallery, (dialog, which) -> startGalleryChooser())
                    .setNegativeButton(R.string.dialog_select_camera, (dialog, which) -> startCamera());
            builder.create().show();
        });

        mImageDetails = findViewById(R.id.image_details);
        mImageDetails.setVisibility(View.INVISIBLE);

        mMainImage = findViewById(R.id.main_image);
        mImageAdvice = findViewById(R.id.image_advice);
        allCheck = findViewById(R.id.all_check);
        allClear = findViewById(R.id.all_clear);
        builder = new AlertDialog.Builder(this);
        //  m_oListView = findViewById(R.id.listv);

        allCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.setTitle("영양정보 저장");
                builder.setMessage("정보를 저장하시겠습니까?");
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(mImageDetails.getText().equals(R.string.not_image_upload) ||
                                mImageDetails.getText().equals(R.string.loading_message) ||
                                mImageDetails.getText().equals(""))
                            Toast.makeText(getApplicationContext(), "영양정보를 다시 확인해주세요!", Toast.LENGTH_SHORT).show();
                        else {
                            //HashMap<String, String> nutInfo 전송
                        }
                    }
                });
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { }});

                AlertDialog TrueOrFalse = builder.create();
                TrueOrFalse.show();
            }
        });
        allClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainImage.setVisibility(View.INVISIBLE);
                mImageAdvice.setVisibility(View.INVISIBLE);
                mImageDetails.setText(R.string.not_image_upload);
                mImageDetails.setEnabled(false);
            }
        });
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);

            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }



    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                MAX_DIMENSION);

                callCloudVision(bitmap);
                mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature labelDetection = new Feature();
                labelDetection.setType("TEXT_DETECTION");
                labelDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(labelDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    private static class LableDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<MainActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(MainActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();
                return convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return "Cloud Vision API request failed. Check logs for details.";
        }

        protected void onPostExecute(String result) {
            MainActivity activity = mActivityWeakReference.get();
            if (activity != null && !activity.isFinishing()) {
                TextView imageDetail = activity.findViewById(R.id.image_details);
                mImageAdvice.setVisibility(View.VISIBLE);
                mImageDetails.setText(result);  // 파싱이 완료되면 사진과 분석데이터가 EditTextView에 나열된다
                mImageDetails.setEnabled(true);
            }
        }
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Switch text to loading
        mImageDetails.setVisibility(View.VISIBLE);
        mImageDetails.setText(R.string.loading_message);
        mImageDetails.setEnabled(false);
        mMainImage.setVisibility(View.VISIBLE);

        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private static String convertResponseToString(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("Found This :\n\n");
        parse = "";         // parse = 카메라로 파싱한 전체 데이터
        parseresult = "";   // parseresult = 파싱한 데이터와 미리 저장한 성분들과 일치하는 문자열만을 저장
        nutInfo.clear();    // HashMap은 사진을 찍어서 파싱할 때마다 초기화되야하므로..
        list.clear();       // ListView 도 초기화..

        List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
        if (labels != null) {
            parse += labels.get(0).getDescription();
            message.append(labels.get(0).getDescription());
            message.append("\n");

        } else {
            message.append("nothing");
        }
        int start=0, end=0;
        for(int i=0; i<foodcategory.length;) {
        //for(int i=0; i<nut.size();) {
            //start = parse.indexOf(nut.get(i), end);
            start = parse.indexOf(foodcategory[i]);
            if(start != -1) {
                end = parse.indexOf("g", start);
                if(parseresult.equals(""))
                    parseresult += parse.substring(start, end + 1);
                else
                    parseresult += "\n"+parse.substring(start, end + 1);
                nutInfo.put(nuteng[i], parse.substring(start, end + 1));

                    /* ListView 에다가 등록하기 위한 과정.. strTitle = 성분(영문), strData = 중량

                    oItem = new ItemData();
                    oItem.strTitle = nut.get(i)+ " (" + nuteng[i]+ ")";
                    oItem.strData = parse.substring(start+nut.get(i).length(), end+1);
                    list.add(oItem);

                     */

                    /* 아마 HashMap에 성분(영문)과 중량을 넣으려고..

                    nutInfo.put(nuteng[i], parse.substring(start+nut.get(i).length(), end+1));

                     */

            }
            else if(start == -1 && i > foodcategory.length)
                break;
            i++;
            //else if(start == -1 && i > nut.size())
                //break;
            //i++;
        }

            /* ListView를 동적으로 만들려고..

            oAdapter = new com.google.sample.cloudvision.ListAdapter(list);
            m_oListView.setAdapter(oAdapter);

             */

        return parseresult;
    }
}
