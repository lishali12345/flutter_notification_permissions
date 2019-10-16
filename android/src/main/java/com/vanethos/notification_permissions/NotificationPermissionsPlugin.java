package com.vanethos.notification_permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class NotificationPermissionsPlugin implements MethodChannel.MethodCallHandler {
  private static final int REQUEST_NOTIFICATION_PERMISSION = 59520;

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel =
        new MethodChannel(registrar.messenger(), "notification_permissions");
    channel.setMethodCallHandler(new NotificationPermissionsPlugin(registrar));
  }

  private static final String PERMISSION_GRANTED = "granted";
  private static final String PERMISSION_DENIED = "denied";

  private final Context context;

  private MethodChannel.Result mResult;

  private NotificationPermissionsPlugin(Registrar registrar) {
    this.context = registrar.activity();
    registrar.addActivityResultListener(new PluginRegistry.ActivityResultListener() {
      @Override
      public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
          if (mResult != null) {
            mResult.success(getNotificationPermissionStatus());
            mResult = null;
          }
          return true;
        }
        return false;
      }
    });
  }

  @Override
  public void onMethodCall(MethodCall call, MethodChannel.Result result) {
    if ("getNotificationPermissionStatus".equalsIgnoreCase(call.method)) {
      result.success(getNotificationPermissionStatus());
    } else if ("requestNotificationPermissions".equalsIgnoreCase(call.method)) {
      if (PERMISSION_DENIED.equalsIgnoreCase(getNotificationPermissionStatus())) {
        if (context instanceof Activity) {
          try {
            mResult = result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                final Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());

                ((Activity) context).startActivityForResult(intent, REQUEST_NOTIFICATION_PERMISSION);
            } else {
              final Uri uri = Uri.fromParts("package", context.getPackageName(), null);

              final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
              intent.setData(uri);

              ((Activity) context).startActivityForResult(intent, REQUEST_NOTIFICATION_PERMISSION);
            }
          } catch (Exception e) {
            result.error(call.method, "failed to open setting", null);
          }
        } else {
          result.error(call.method, "context is not instance of Activity", null);
          mResult = null;
        }
      } else {
        result.success(PERMISSION_DENIED);
      }
    } else {
      result.notImplemented();
    }
  }

  private String getNotificationPermissionStatus() {
    return (NotificationManagerCompat.from(context).areNotificationsEnabled())
        ? PERMISSION_GRANTED
        : PERMISSION_DENIED;
  }
}
