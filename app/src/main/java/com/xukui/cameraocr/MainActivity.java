package com.xukui.cameraocr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.xukui.cameraocr.utils.permission.PermissionUtil;
import com.yanzhenjie.permission.runtime.Permission;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.idcardOcr_btn).setOnClickListener(v -> {
            PermissionUtil.requestPermission(this, data -> {
                Intent intent = new Intent(MainActivity.this, IdcardOcrActivity.class);
                startActivity(intent);
            }, new String[]{Permission.CAMERA});
        });

        findViewById(R.id.faceOcr_btn).setOnClickListener(v -> {
        });
    }

}