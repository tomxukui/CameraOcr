package com.xukui.cameraocr.utils.permission;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.xukui.cameraocr.Mapp;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.option.Option;
import com.yanzhenjie.permission.runtime.PermissionDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Created by xukui on 2019-03-20.
 */
public class PermissionUtil {

    /**
     * 请求权限
     */
    public static void requestPermission(Context context, Action<List<String>> granted, @Nullable Action<List<String>> denied, @PermissionDef String[] permissions) {
        requestPermission(context, AndPermission.with(context), granted, denied, permissions);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Context context, Action<List<String>> granted, @PermissionDef String[] permissions) {
        requestPermission(context, granted, getDeniedAction(), permissions);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Activity activity, Action<List<String>> granted, @Nullable Action<List<String>> denied, @PermissionDef String[] permissions) {
        requestPermission(activity, AndPermission.with(activity), granted, denied, permissions);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Activity activity, Action<List<String>> granted, @PermissionDef String[] permissions) {
        requestPermission(activity, granted, getDeniedAction(), permissions);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Fragment fragment, Action<List<String>> granted, @Nullable Action<List<String>> denied, @PermissionDef String[] permissions) {
        requestPermission(fragment.getContext(), AndPermission.with(fragment), granted, denied, permissions);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Fragment fragment, Action<List<String>> granted, @PermissionDef String[] permissions) {
        requestPermission(fragment, granted, getDeniedAction(), permissions);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Context context, Option option, Action<List<String>> granted, @Nullable Action<List<String>> denied, @PermissionDef String[] permissions) {
        if (AndPermission.hasPermissions(context, permissions)) {
            if (granted != null) {
                granted.onAction(Arrays.asList(permissions));
            }

        } else {
            option.runtime()
                    .permission(permissions)
                    .rationale(new RuntimeRationale())
                    .onGranted(granted)
                    .onDenied(data -> {
                        if (AndPermission.hasPermissions(context, permissions)) {
                            if (granted != null) {
                                granted.onAction(data);
                            }

                        } else {
                            if (denied != null) {
                                denied.onAction(data);
                            }
                        }
                    })
                    .start();
        }
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Context context, Action<List<String>> granted, @Nullable Action<List<String>> denied, String[][] groups) {
        requestPermission(context, AndPermission.with(context), granted, denied, groups);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Context context, Action<List<String>> granted, String[][] groups) {
        requestPermission(context, granted, getDeniedAction(), groups);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Activity activity, Action<List<String>> granted, @Nullable Action<List<String>> denied, String[][] groups) {
        requestPermission(activity, AndPermission.with(activity), granted, denied, groups);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Activity activity, Action<List<String>> granted, String[][] groups) {
        requestPermission(activity, granted, getDeniedAction(), groups);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Fragment fragment, Action<List<String>> granted, @Nullable Action<List<String>> denied, String[][] groups) {
        requestPermission(fragment.getContext(), AndPermission.with(fragment), granted, denied, groups);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Fragment fragment, Action<List<String>> granted, String[][] groups) {
        requestPermission(fragment, granted, getDeniedAction(), groups);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Context context, Option option, Action<List<String>> granted, @Nullable Action<List<String>> denied, String[][] groups) {
        if (AndPermission.hasPermissions(context, groups)) {
            if (granted != null) {
                granted.onAction(groups2List(groups));
            }

        } else {
            option.runtime()
                    .permission(groups)
                    .rationale(new RuntimeRationale())
                    .onGranted(granted)
                    .onDenied(data -> {
                        if (AndPermission.hasPermissions(context, groups)) {
                            if (granted != null) {
                                granted.onAction(data);
                            }

                        } else {
                            if (denied != null) {
                                denied.onAction(data);
                            }
                        }
                    })
                    .start();
        }
    }

    private static Action<List<String>> getDeniedAction() {
        return data -> Toast.makeText(Mapp.getInstance(), "权限获取失败", Toast.LENGTH_SHORT).show();
    }

    private static List<String> groups2List(String[]... groups) {
        List<String> list = new ArrayList<>();

        if (groups != null) {
            for (int i = 0; i < groups.length; i++) {
                String[] items = groups[i];

                if (items != null) {
                    list.addAll(Arrays.asList(items));
                }
            }
        }

        return list;
    }

}