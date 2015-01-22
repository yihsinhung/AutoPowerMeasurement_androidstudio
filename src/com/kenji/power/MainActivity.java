package com.kenji.power;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener {

	String galleryPackagename = "";
	private static final int REQUEST_CODE_DEVICE_ADMIN = 20;

	private int testType;

	Button step1MusicButton;
	Button step1VideoButton;
	Button step2FastButton;
	Button step3CompleteButton;
	Button step4StopButton;

	public static final String TAG = "Auto Power Measurement";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViews();
		getPackages();

	}

	private void findViews() {
		step1MusicButton = (Button) findViewById(R.id.buttonStep1Music);
		step1VideoButton = (Button) findViewById(R.id.buttonStep1Video);
		step2FastButton = (Button) findViewById(R.id.buttonStep2Fast);
		step3CompleteButton = (Button) findViewById(R.id.buttonStep3Complete);
		step4StopButton = (Button) findViewById(R.id.buttonStep4Stop);

		step1MusicButton.setOnClickListener(this);
		step1VideoButton.setOnClickListener(this);
		step2FastButton.setOnClickListener(this);
		step3CompleteButton.setOnClickListener(this);
		step4StopButton.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonStep1Music:
			Intent intentMusic = new Intent();
			intentMusic.setAction(Intent.ACTION_VIEW);
			File fileMusic = new File(Environment.getExternalStorageDirectory()
					.getPath() + "/Music/" + "1. Bitter Heart.mp3");
			intentMusic.setDataAndType(Uri.fromFile(fileMusic), "audio/*");
			intentMusic.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intentMusic.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intentMusic);

			break;
		case R.id.buttonStep1Video:
			Intent intentVideo = new Intent();
			if (galleryPackagename.equals("com.android.gallery3d")) {
				ComponentName comp = new ComponentName(galleryPackagename,
						galleryPackagename + ".app.MovieActivity");
				intentVideo.setComponent(comp);
			}
			intentVideo.setAction(Intent.ACTION_VIEW);
			File fileVideo = new File(Environment.getExternalStorageDirectory()
					.getPath() + "/H264_1080p_15Mbps_30fps.mp4");
			intentVideo.setDataAndType(Uri.fromFile(fileVideo), "video/*");
			intentVideo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intentVideo.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intentVideo);

			break;
		case R.id.buttonStep2Fast:
			testType = APMService.TEST_TYPE_QUICK;
			startTesting(testType);

			break;
		case R.id.buttonStep3Complete:
			testType = APMService.TEST_TYPE_FULL;
			startTesting(testType);

			break;
		case R.id.buttonStep4Stop:
			if (isMyServiceRunning()) {
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(), APMService.class);
				getApplicationContext().stopService(intent);
			} else {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.main_no_testing),
						Toast.LENGTH_SHORT).show();
			}

			break;

		}

	}

	private void startTesting(int testType) {
		ComponentName componentName = new ComponentName(
				getApplicationContext(), deviceAdminReceiver.class);
		DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		boolean isAdminActive = devicePolicyManager
				.isAdminActive(componentName);
		if (!isAdminActive) {
			Intent intent = new Intent(
					DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
					componentName);
			// intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
			// "（自定義區域2）");
			startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN);
		} else {

			if (isMyServiceRunning()) {
				Toast.makeText(
						getApplicationContext(),
						getResources().getString(
								R.string.main_device_is_testing),
						Toast.LENGTH_SHORT).show();
			} else {
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("gallery", galleryPackagename);
				bundle.putInt(APMService.ACTIVITY_PARA_TEST_TYPE, testType);
				bundle.putInt(APMService.ACTIVITY_PARA_ORIENTATION,
						getDeviceDefaultOrientation());
				intent.putExtras(bundle);
				intent.setClass(getApplicationContext(), APMService.class);
				getApplicationContext().startService(intent);
				finish();
			}

		}
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (APMService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	class PInfo {
		private String pname = "";

		private void prettyPrint() {
			if (pname.contains("gallery")) {
				galleryPackagename = pname;
				Log.d("Kenji", pname);
			}

		}
	}

	private ArrayList<PInfo> getPackages() {
		ArrayList<PInfo> apps = getInstalledApps(true); /*
														 * false = no system
														 * packages
														 */
		final int max = apps.size();
		for (int i = 0; i < max; i++) {
			apps.get(i).prettyPrint();
		}
		return apps;
	}

	private ArrayList<PInfo> getInstalledApps(boolean getSysPackages) {
		ArrayList<PInfo> res = new ArrayList<PInfo>();
		List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
		for (int i = 0; i < packs.size(); i++) {
			PackageInfo p = packs.get(i);
			if ((!getSysPackages) && (p.versionName == null)) {
				continue;
			}
			PInfo newInfo = new PInfo();
			// newInfo.appname =
			// p.applicationInfo.loadLabel(getPackageManager())
			// .toString();
			newInfo.pname = p.packageName;
			// newInfo.versionName = p.versionName;
			// newInfo.versionCode = p.versionCode;
			// newInfo.icon = p.applicationInfo.loadIcon(getPackageManager());
			res.add(newInfo);
		}
		return res;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_DEVICE_ADMIN && resultCode == RESULT_OK) {
			startTesting(testType);
		}
	}

	public int getDeviceDefaultOrientation() {

		WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		Configuration config = getResources().getConfiguration();

		int rotation = windowManager.getDefaultDisplay().getRotation();

		if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && config.orientation == Configuration.ORIENTATION_LANDSCAPE)
				|| ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
			return Configuration.ORIENTATION_LANDSCAPE;
		} else {
			return Configuration.ORIENTATION_PORTRAIT;
		}
	}
}
