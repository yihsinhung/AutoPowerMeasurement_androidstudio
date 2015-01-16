package com.kenji.power;

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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	String galleryPackagename = "";
	private static final int REQUEST_CODE_DEVICE_ADMIN = 20;

	private int testType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button btnStartShort = (Button) findViewById(R.id.button1);
		Button btnStart = (Button) findViewById(R.id.button2);
		Button btnStop = (Button) findViewById(R.id.button3);

		btnStartShort.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				testType = APMService.TEST_TYPE_QUICK;
				startTesting(testType);
			}
		});

		btnStart.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				testType = APMService.TEST_TYPE_FULL;
				startTesting(testType);
			}
		});
		btnStop.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (isMyServiceRunning()) {
					Intent intent = new Intent();
					intent.setClass(getApplicationContext(), APMService.class);
					getApplicationContext().stopService(intent);
				} else {
					Toast.makeText(getApplicationContext(),
							getResources().getString(R.string.main_no_testing),
							Toast.LENGTH_SHORT).show();
				}

			}
		});

		getPackages();
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

}
