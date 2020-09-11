package com.example.k_means_clustering;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private static final String TAG = null;
    private ImageView imageView;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static int cluster_no,cluster_count;
    Bitmap photo;Button sendData;int k,data_point;
    private static Map<Integer, Integer> counts;
    private static HashMap<Integer, List<Pixels>> hashMap;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.imageView = (ImageView)this.findViewById(R.id.imageView);
        Button photoButton = (Button) this.findViewById(R.id.Take_Pic);
        final Button kMeanButton = (Button) this.findViewById(R.id.K_mean);
        sendData  = (Button) this.findViewById(R.id.sendData);

        kMeanButton.setEnabled(false);
        sendData.setVisibility(View.GONE);

       //Run time Permission to read external storage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        //Run time Permission to write external storage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        //Take image button click operation
        photoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                    {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                    }
                    else
                    {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    }
                    kMeanButton.setEnabled(true);
                }
            }
        });

        //Apply K-Mean button click operation
        kMeanButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
             OpenCVLoader.initDebug();
             showAlert();
            }
        });

    }


    //After the request permission is granted open camera and take picture
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

      //loaded OpenCV
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    Mat imageMat = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    // After the image is taken save the imaage
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            photo = (Bitmap) data.getExtras().get("data");
            try {
                saveImage(photo);
                imageView.setImageBitmap(photo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //function to show alert box and take no of cluster input from the user and run K-Means
    public void showAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alertdialog_custom_view,null);

        // Specify alert dialog is not cancelable/not ignorable
        builder.setCancelable(false);

        // Set the custom layout as alert dialog view
        builder.setView(dialogView);

        // Get the custom alert dialog view widgets reference
        Button btn_positive = (Button) dialogView.findViewById(R.id.dialog_positive_btn);
        Button btn_negative = (Button) dialogView.findViewById(R.id.dialog_negative_btn);
        final EditText k_value = (EditText) dialogView.findViewById(R.id.et_name);

        // Create the alert dialog
        final AlertDialog dialog = builder.create();

        // Set positive/yes button click listener
        btn_positive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Dismiss the alert dialog
                dialog.cancel();
                k= Integer.parseInt(k_value.getText().toString());
                Toast.makeText(getApplication(),
                        k+" Number of clusters are selected", Toast.LENGTH_LONG).show();
                try {
                    k_Mean(photo,k);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        // Set negative/no button click listener
        btn_negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(getApplication(),
                        "No value of K is selected", Toast.LENGTH_LONG).show();
            }
        });

        // Display the custom alert dialog on interface
        dialog.show();
    }

    //function to save the image in the Pictures Folder of the external storage
    public void saveImage(Bitmap photo) throws IOException {

        final String relativePath = Environment.DIRECTORY_PICTURES + File.separator + "Pictures"; // save directory
        String fileName = "Your_File_Name"; // file name to save file with
        String mimeType = "image/*"; // Mime Types define here
        Bitmap bitmap = photo; // your bitmap file to save

        final ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE,mimeType );

        final ContentResolver resolver = getApplicationContext().getContentResolver();

        OutputStream stream = null;
        Uri uri = null;

        try {
            final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = resolver.insert(contentUri, contentValues);

            if (uri == null) {
                Log.d("error", "Failed to create new  MediaStore record.");
                return;
            }

            stream = resolver.openOutputStream(uri);

            if (stream == null) {
                Log.d("error", "Failed to get output stream.");
            }

            boolean saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            if (!saved) {
                Log.d("error", "Failed to save bitmap.");
            }
        } catch (IOException e) {
            if (uri != null) {
                resolver.delete(uri, null, null);
            }

        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    //function to save the image data in the .txt file on the SDcard
    private String saveDataOnSDcard(List<Mat> clusters, final int cluster_no, int data_point) {

        String path =  getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)+File.separator+"Cluster_Data"+".txt";
        File file = new File(path);

        try {
            FileOutputStream out = new FileOutputStream(file);
           // Collections.shuffle(hashMap.get(cluster_no));
            //List<Pixels> randomSeries = hashMap.get(cluster_no).subList(0, data_point);
            List<Pixels> randomSeries;

         /*   for (Pixels p : randomSeries) {
                System.out.println("RGBBBB: "+p.getR()+":"+p.getG()+":"+p.getB());
                //out.write((p.getR()+","+p.getG()+","+p.getB()+" ").getBytes());
                out.write((p.getCoordinates()[0]+","+p.getCoordinates()[1]+"   ").getBytes());
            }*/

            for(int i=0; i<k; i++){
                Collections.shuffle(hashMap.get(i));

                if(hashMap.get(i).size()<data_point){
                    randomSeries = hashMap.get(i).subList(0,hashMap.get(i).size());
                }
                else{
                   randomSeries = hashMap.get(i).subList(0, data_point);
                }

                out.write(("Cluster number: "+i).getBytes());
                out.write("\r\n".getBytes());
                for (Pixels p : randomSeries) {
                    System.out.println("RGBBBB: "+p.getR()+":"+p.getG()+":"+p.getB());
                    //out.write((p.getR()+","+p.getG()+","+p.getB()+" ").getBytes());
                    out.write((p.getCoordinates()[0]+","+p.getCoordinates()[1]+"  ").getBytes());
                }
                out.write("\r\n".getBytes());
                randomSeries.clear();
            }


            out.flush();
            out.close();
            Toast.makeText(this, "Data is loaded into the text file", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Data is not shared successfully", Toast.LENGTH_LONG).show();
        }
        return file.getPath();
    }

    //function to share the image cluster data on the shared interface via gmail,facebook,whatsapp etc
    public void onShareOnePhoto(String filePath) {

        Uri path = FileProvider.getUriForFile(this, "com.example.k_means_clustering",  new File(filePath));

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "The cluster data is attached");
        shareIntent.putExtra(Intent.EXTRA_STREAM, path);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setType("text/*");
        startActivity(Intent.createChooser(shareIntent, "Share..."));
    }

    //fucntion is run after the send data button is clicked
    private void sendData(final List<Mat> clusters) {

        sendData.setVisibility(View.VISIBLE);

        sendData.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showAlertData(clusters);
            }
        });
    }

    //display second alert box after send data button is clicked
    private void showAlertData(final List<Mat> clusters ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alertdatadialog_custom_view,null);

        // Specify alert dialog is not cancelable/not ignorable
        builder.setCancelable(false);

        // Set the custom layout as alert dialog view
        builder.setView(dialogView);

        // Get the custom alert dialog view widgets reference
        Button btn_positive = (Button) dialogView.findViewById(R.id.dialog_positive_btn);
        Button btn_negative = (Button) dialogView.findViewById(R.id.dialog_negative_btn);

        final EditText cluster_number = (EditText) dialogView.findViewById(R.id.et_name1);
        final EditText data_range = (EditText) dialogView.findViewById(R.id.et_name2);

        // Create the alert dialog
        final AlertDialog dialog = builder.create();

        // Set positive/yes button click listener
        btn_positive.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // Dismiss the alert dialog
                dialog.cancel();
                cluster_no = Integer.parseInt(cluster_number.getText().toString());
                cluster_count = counts.get(cluster_no);
                data_point = Integer.parseInt(data_range.getText().toString());

                /*if(cluster_no < k && cluster_no >= 0 ) {
                    if(cluster_count >= data_point && data_point!=0){*/
                        Toast.makeText(getApplication(),
                                data_point + " points from clusters are ready to send.", Toast.LENGTH_LONG).show();
                        String path = saveDataOnSDcard(clusters, cluster_no, data_point);
                        onShareOnePhoto(path);
                   /* }
                    else{
                        Toast.makeText(getApplication(), "The total data points present in the cluster " +cluster_no+ " is "+cluster_count+". However you have entered "+data_point+ " data points.", Toast.LENGTH_LONG).show();
                    }
                  }
                else{
                    Toast.makeText(getApplication(),
                            "Cluster number you enter is bigger or smaller then the no of clusters in the data", Toast.LENGTH_LONG).show();
                }*/
            }
        });

        // Set negative/no button click listener
        btn_negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(getApplication(),
                        "No value is selected", Toast.LENGTH_LONG).show();
            }
        });
        // Display the custom alert dialog on interface
        dialog.show();
    }

//function to run open cv k mean clustering algorithm on the image taken by the camera
    public void k_Mean(Bitmap photo, int k) throws IOException {
        Mat rgba = new Mat();
        Mat mHSV = new Mat();

       // Bitmap outputBitmap = Bitmap.createBitmap(photo.getWidth(),photo.getHeight(), Bitmap.Config.RGB_565);
        Bitmap outputBitmap = Bitmap.createBitmap(photo.getWidth(),photo.getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat(photo,rgba);

        //must convert to 3 channel image
        Imgproc.cvtColor(rgba, mHSV, Imgproc.COLOR_RGBA2RGB,3);
        Imgproc.cvtColor(rgba, mHSV, Imgproc.COLOR_RGB2HSV,3);
        List<Mat> clusters = clusterList(mHSV,k);
        //Mat clusters = cluster(mHSV,k).get(0);
        Utils.matToBitmap(clusterList(mHSV,k).get(0),outputBitmap);

        imageView.setImageBitmap(outputBitmap);
        saveImage(outputBitmap);
        sendData(clusters);
    }

    //function to list down the clusters
    public static List<Mat> clusterList(Mat cutout, int k) {
        Mat samples = cutout.reshape(1, cutout.cols() * cutout.rows());
        Mat samples32f = new Mat();
        samples.convertTo(samples32f, CvType.CV_32F, 1.0 / 255.0);

        Mat labels = new Mat();

        //criteria means the maximum loop
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT, 20, 1);
        Mat centers = new Mat();
        Core.kmeans(samples32f, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);

        return displyClusters(cutout, labels, centers);
    }

    //function to show clusters
    public static List<Mat> displyClusters(Mat cutout, Mat labels, Mat centers) {
        centers.convertTo(centers, CvType.CV_8UC1, 255.0);
        centers.reshape(3);

        List<Mat> clusters = new ArrayList<Mat>();
        for(int i = 0; i < centers.rows(); i++) {
            clusters.add(Mat.zeros(cutout.size(), cutout.type()));
        }

        counts = new HashMap<>();
        for(int i = 0; i < centers.rows(); i++)
            counts.put(i, 0);

        int rows = 0;
        hashMap =new HashMap<>();
        byte[] byt = new byte[3];
        for(int y = 0; y < cutout.rows(); y++) {
            for(int x = 0; x < cutout.cols(); x++) {
                int[] coordinates = new int[2];
                int label = (int)labels.get(rows, 0)[0];
                int r = (int)centers.get(label, 2)[0];
                int g = (int)centers.get(label, 1)[0];
                int b = (int)centers.get(label, 0)[0];
                coordinates[0] = y;
                coordinates[1] = x;

             //   System.out.println("x: " +x + "\ty: "+y);// returned the (x,y) //co ordinates of all white pixels.
                Pixels pix = new Pixels();
                pix.setR(x);
                pix.setG(g);
                pix.setB(b);
                pix.setCoordinates(coordinates);

                if (!hashMap.containsKey(label)) {
                    List<Pixels> list = new ArrayList<Pixels>();
                    list.add(pix);
                    hashMap.put(label, list);
                } else {
                    hashMap.get(label).add(pix);
                }

               // System.out.println("Label: "+label+" B: "+b+" G: "+g+" R: "+r);
                 counts.put(label, counts.get(label) + 1);
                clusters.get(label).put(y, x, b, g, r);
                rows++;
            }
        }
        System.out.println("Counts: "+counts);
        return clusters;
    }
}
