package converter.dassem.com.fileconverter;

import android.app.Activity;
import android.content.pm.PackageManager;

import com.appodeal.ads.utils.PermissionsHelper;

class AppodealPermissionCallbacks implements PermissionsHelper.AppodealPermissionCallbacks {
    private final Activity mActivity;

    AppodealPermissionCallbacks(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void writeExternalStorageResponse(int result) {
        if (result == PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(mActivity, "WRITE_EXTERNAL_STORAGE permission was granted",Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(mActivity, "WRITE_EXTERNAL_STORAGE permission was NOT granted",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void accessCoarseLocationResponse(int result) {
        if (result == PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(mActivity, "ACCESS_COARSE_LOCATION permission was granted",Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(mActivity, "ACCESS_COARSE_LOCATION permission was NOT granted",Toast.LENGTH_SHORT).show();
        }
    }
}