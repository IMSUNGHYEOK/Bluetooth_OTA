package com.qualcomm.qti.EMTechOTA.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.qti.EMTechOTA.R;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CustomUpgradeActivity extends Activity implements View.OnClickListener{
    Button btn_File;
    Button btn_URL;
    Button btn_Down;
    EditText edit_URL;
    TextView txt_Mode;
    TextView txt_Manual;

    int btn_UrlState = -1; //off 버튼
    int btn_FileState = -1; //off 버튼

    int down_state = -1;

    String TestURL = "https://www.dropbox.com/s/w8gsloo0acpc72w/output.zip?dl=1";

    public static final String WIFI_STATE = "WIFE";
    public static final String MOBILE_STATE = "MOBILE";
    public static final String NONE_STATE = "NONE";

    public static String getWhatKindOfNetwork(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return WIFI_STATE;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return MOBILE_STATE;
            }
        }
        return NONE_STATE;
    }


    private ProgressDialog progressBar;



    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE"};
    private File outputFile; //파일명까지 포함한 경로
    private File path;//디렉토리경로

    public void changeManual(int index){
        switch (index){
            case 1:
                txt_Mode.setText("Select DownLoad Mode");
                txt_Manual.setText("1. Upgrade 파일 설치 방법을 선택하세요."+"\n"+"\n"+"\n"+
                                "A. URL DOWNLOAD - URL 주소를 입력받아 설치합니다."+"\n"+
                                "B. DEVICE DOWNLOAD - 휴대폰에 설치된 파일을 사용합니다.");
                btn_UrlState = -1;
                btn_FileState = -1;
                edit_URL.setVisibility(View.GONE);
                btn_File.setVisibility(View.VISIBLE);
                btn_Down.setVisibility(View.GONE);
                break;
            case 2:
                txt_Mode.setText("URL DOWNLOAD MODE");
                txt_Manual.setText("2. Upgrade 파일 설치 받을 URL을 입력하고 DOWNLOAD 버튼을 눌러주세요."+"\n"+"\n"+"\n"+
                        "   A. URL DOWNLOAD 버튼 - 돌아가기");
                edit_URL.setVisibility(View.VISIBLE);
                btn_File.setVisibility(View.GONE);
                btn_Down.setVisibility(View.VISIBLE);
                btn_UrlState = 0;
                break;
            case 3:
                Intent intentUpgrade2 = new Intent(this, UpgradeActivity.class);
                startActivity(intentUpgrade2);

                break;
            case 4:
                txt_Manual.setText("");
                break;
        }
    }

    public void checkInternet(String index){
        if(index==WIFI_STATE){
            Toast.makeText(getApplicationContext(),"Wifi를 사용하여 설치합니다.",Toast.LENGTH_SHORT).show();
        }else if(index==MOBILE_STATE){
            Toast.makeText(getApplicationContext(),"모바일 데이터를 사용하여 설치합니다.",Toast.LENGTH_SHORT).show();
        }else if(index==NONE_STATE){
            Toast.makeText(getApplicationContext(),"인터넷 사용이 불가능합니다.",Toast.LENGTH_SHORT).show();
            changeManual(1);
            return;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_upgrade);
        down_state = -1;
        txt_Mode = (TextView) findViewById(R.id.txt_mode);
        txt_Manual = (TextView) findViewById(R.id.txt_Manual);
        edit_URL = (EditText) findViewById(R.id.edit_URL);
        edit_URL.setText("");
        btn_URL = (Button) findViewById(R.id.btn_URL);
        btn_URL.setOnClickListener(this);
        btn_File = (Button) findViewById(R.id.btn_File);
        btn_File.setOnClickListener(this);
        btn_Down = (Button) findViewById(R.id.btn_DownLoad);
        btn_Down.setOnClickListener(this);

        progressBar=new ProgressDialog(CustomUpgradeActivity.this);
        progressBar.setMessage("Download..");
        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressBar.setIndeterminate(true);
        progressBar.setCancelable(true);

        changeManual(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        edit_URL.setText("");
        down_state = -1;
        btn_UrlState = -1;
        btn_FileState = -1;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.btn_URL:
                if(btn_UrlState == -1){
                    changeManual(2);
                }else if(btn_UrlState == 0){
                    changeManual(1);
                    btn_UrlState = -1;
                }
                break;
            case R.id.btn_File:
                if(btn_FileState == -1){
                    Intent intentUpgrade = new Intent(this, UpgradeActivity.class);
                    startActivity(intentUpgrade);
                    changeManual(1);
                }
                break;
            case R.id.btn_DownLoad:
                if(edit_URL.getText().toString().length()>=10) {
                    FileDownload();
                }else if(edit_URL.getText().toString().length()<10)
                    Toast.makeText(getApplicationContext(),"유효하지 않은 URL 입니다.",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private boolean hasPermissions(String[] permissions) {
        int res = 0;
        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){
            res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)){
                //퍼미션 허가 안된 경우
                return false;
            }

        }
        //퍼미션이 허가된 경우
        return true;
    }

    private void requestNecessaryPermissions(String[] permissions) {
        //마시멜로( API 23 )이상에서 런타임 퍼미션(Runtime Permission) 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    public void FileDownload(){

        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        Log.i("PATH" ,path.getPath().toString());
        //path= Environment.getExternalStoragePublicDirectory("/gaia");
        outputFile= new File(path, "Output.zip");

        if(outputFile.exists()){
            AlertDialog.Builder builder = new AlertDialog.Builder(CustomUpgradeActivity.this);
            builder.setTitle("File Download");
            builder.setMessage("디렉토리에 이미 파일이 있습니다."+"\n"+"새로 설치하겠습니까?");
            builder.setNegativeButton("No",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            Toast.makeText(getApplicationContext(),"기존의 파일을 사용합니다.",Toast.LENGTH_SHORT).show();
                            down_state = 0;
                            changeManual(3);
                            //playVideo(outputFile.getPath()); unzip
                        }
                    });
            builder.setPositiveButton("Yes",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            outputFile.delete();

                            final DownloadFilesTask downloadTask = new DownloadFilesTask(CustomUpgradeActivity.this);
                            //changeManual(3);
                            checkInternet(getWhatKindOfNetwork(CustomUpgradeActivity.this));
                            downloadTask.execute(edit_URL.getText().toString());
                            edit_URL.setText("");
                            progressBar.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    Toast.makeText(getApplicationContext(), "설치가 취소되었습니다.", Toast.LENGTH_SHORT).show();
                                    downloadTask.cancel(true);
                                    changeManual(1);
                                }
                            });
                        }
                    });
            builder.show();
        }else{
            //changeManual(3);
            checkInternet(getWhatKindOfNetwork(CustomUpgradeActivity.this));
            final DownloadFilesTask downloadTask = new DownloadFilesTask(CustomUpgradeActivity.this);
            downloadTask.execute(edit_URL.getText().toString());
            edit_URL.setText("");
            progressBar.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Toast.makeText(getApplicationContext(), "설치가 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    downloadTask.cancel(true);
                    changeManual(1);
                }
            });
        }
    }



    public class DownloadFilesTask extends AsyncTask<String, String, Long>{
        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadFilesTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();

            progressBar.show();
        }

        @Override
        protected Long doInBackground(String... string_url) {
            int count;
            long FileSize = -1;
            InputStream input = null;
            OutputStream output = null;
            URLConnection connection = null;
            try {
                URL url = new URL(string_url[0]);
                connection = url.openConnection();
                connection.connect();


                //파일 크기를 가져옴
                FileSize = connection.getContentLength();

                //URL 주소로부터 파일다운로드하기 위한 input stream
                input = new BufferedInputStream(url.openStream(), 8192);

                path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                outputFile= new File(path, "output.zip"); //파일명까지 포함함 경로의 File 객체 생성

                // SD카드에 저장하기 위한 Output stream
                output = new FileOutputStream(outputFile);


                byte data[] = new byte[1024];
                long downloadedSize = 0;
                while ((count = input.read(data)) != -1) {
                    //사용자가 BACK 버튼 누르면 취소가능
                    if (isCancelled()) {
                        input.close();
                        return Long.valueOf(-1);
                    }

                    downloadedSize += count;

                    if (FileSize > 0) {
                        float per = ((float)downloadedSize/FileSize) * 100;
                        String str = "Downloaded " + downloadedSize + "KB / " + FileSize + "KB (" + (int)per + "%)";
                        publishProgress("" + (int) ((downloadedSize * 100) / FileSize), str);

                    }

                    //파일에 데이터를 기록합니다.
                    output.write(data, 0, count);
                }
                // Flush output
                output.flush();

                // Close streams
                output.close();
                input.close();


            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                mWakeLock.release();

            }
            return FileSize;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate(progress);
            progressBar.setIndeterminate(false);
            progressBar.setMax(100);
            progressBar.setProgress(Integer.parseInt(progress[0]));
            progressBar.setMessage(progress[1]);
        }

        @Override
        protected void onPostExecute(Long size) {
            super.onPostExecute(size);
            progressBar.dismiss();

            if ( size > 0) {
                Toast.makeText(getApplicationContext(), "Success DownLoad. File Size=" + size.toString(), Toast.LENGTH_LONG).show();

                Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(outputFile));
                sendBroadcast(mediaScanIntent);

                //playVideo(outputFile.getPath());

                Decompress unzip = new Decompress(CustomUpgradeActivity.this, outputFile);
                unzip.unzip();
                changeManual(3);
                down_state=0;
                //unpackZip(Environment.getExternalStorageDirectory().getPath()+"/download");
                Log.i("@@@pathhhh1 : ",Environment.getExternalStorageDirectory().getPath()+"/download/output");
                Log.i("@@@pathhhh2 : ",Environment.getExternalStorageDirectory().getPath()+"/download/output.zip" );


            }
            else
            {
                Toast.makeText(getApplicationContext(), "유효하지 않은 URL입니다.", Toast.LENGTH_LONG).show();
                changeManual(1);
                down_state = -1;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch(permsRequestCode){

            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean readAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if ( !readAccepted || !writeAccepted  )
                        {
                            showDialogforPermission("앱을 실행하려면 퍼미션을 허가해야합니다.");
                            return;
                        }
                    }
                }
                break;
        }
    }

    private void showDialogforPermission(String msg) {

        final AlertDialog.Builder myDialog = new AlertDialog.Builder(  CustomUpgradeActivity.this);
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
                }

            }
        });
        myDialog.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        myDialog.show();
    }

    public class Decompress {
        private File _zipFile;
        private InputStream _zipFileStream;
        private Context context;
        private static final String ROOT_LOCATION = "/storage/emulated/0/download" ;
        private static final String TAG = "UNZIP";

        public Decompress(Context context, File zipFile) {
            _zipFile = zipFile;
            this.context = context;

            _dirChecker("");
        }

        public Decompress(Context context, InputStream zipFile) {
            _zipFileStream = zipFile;
            this.context = context;

            _dirChecker("");
        }

        public void unzip() {
            try  {
                Log.i(TAG, "Starting to unzip");
                InputStream fin = _zipFileStream;
                if(fin == null) {
                    fin = new FileInputStream(_zipFile);
                }
                ZipInputStream zin = new ZipInputStream(fin);
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    Log.v(TAG, "Unzipping " + ze.getName());

                    if(ze.isDirectory()) {
                        _dirChecker(ROOT_LOCATION + "/" + ze.getName());
                    } else {
                        FileOutputStream fout = new FileOutputStream(new File(ROOT_LOCATION, ze.getName()));
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int count;

                        // reading and writing
                        while((count = zin.read(buffer)) != -1)
                        {
                            baos.write(buffer, 0, count);
                            byte[] bytes = baos.toByteArray();
                            fout.write(bytes);
                            baos.reset();
                        }

                        fout.close();
                        zin.closeEntry();
                    }

                }
                zin.close();
                Log.i(TAG, "Finished unzip");
            } catch(Exception e) {
                Log.e(TAG, "Unzip Error", e);
            }

        }

        private void _dirChecker(String dir) {
            File f = new File(dir);
            Log.i(TAG, "creating dir " + dir);

            if(dir.length() >= 0 && !f.isDirectory() ) {
                f.mkdirs();
            }
        }
    }
}


