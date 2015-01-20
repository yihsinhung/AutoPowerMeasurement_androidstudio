package com.kenji.power;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

@SuppressLint("InlinedApi")
public class APMService extends Service {

	public static final String ACTIVITY_PARA_TEST_TYPE = "activity_para_test_type";
	public static final int TEST_TYPE_QUICK = 0;
	public static final int TEST_TYPE_FULL = 1;
	public static final int TEST_TYPE_SINGLE = 2;
	private int testType;

	private static final long DURATION_ZERO = 0;
	private static final long DURATION_NORMAL = 490000;
	private static final long DURATION_MUSIC = 160000;
	private static final long DURATION_VIDEO_GOLDEN = 110000;
	private static final long DURATION_VIDEO_CAR = 35000;
	private static final long DURATION_LONG = 360000;

	private static final long DURATION_SMALL_DELAY = 500;

	private static final long DURATION_QUICK = 5000;

	Handler mHandler = new Handler();

	PowerManager pm;
	WakeLock wakeLock;

	View switchOrientationView;
	WindowManager wm;

	String gallery = "";

	private int currentPosition;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private String emailContent = "";

	private void setupNextTask(boolean forceExecute) {
		if (currentPosition + 1 < mPowerMeasurementItems.size()
				&& (mPowerMeasurementItems.get(currentPosition)
						.getExpiredDuration() != DURATION_ZERO) || forceExecute) {
			currentPosition++;
			setupPendingIntent(currentPosition);
		} else {
			Log.w("Kenji", "提醒視窗跳出");
			emailContent = emailContent + "提醒視窗跳出" + " time="
					+ sdf.format(new Date(System.currentTimeMillis())) + '\n';
		}
	}

	private ArrayList<PowerMeasurementItem> mPowerMeasurementItems = new ArrayList<PowerMeasurementItem>();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO do something useful
		gallery = intent.getExtras().getString("gallery", "");
		testType = intent.getExtras().getInt(ACTIVITY_PARA_TEST_TYPE,
				TEST_TYPE_QUICK);
		Log.w("Kenji", "testType=" + testType);

		return Service.START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		setupMeasureItems();

		currentPosition = 0;
		setupPendingIntent(currentPosition);

	}

	private void setupMeasureItems() {
		// TODO Auto-generated method stub
		mPowerMeasurementItems.add(mShowStartDialogItem);
		mPowerMeasurementItems.add(mShowHomeItem);
		mPowerMeasurementItems.add(mShowSettingItem);
		mPowerMeasurementItems.add(mMusicFirstTimeItem);
		mPowerMeasurementItems.add(mMusicItem);
		mPowerMeasurementItems.add(mMusicItem);
		mPowerMeasurementItems.add(mVideoGoldenFirstTimeItem);
		mPowerMeasurementItems.add(mVideoGoldenItem);
		mPowerMeasurementItems.add(mVideoGoldenItem);
		mPowerMeasurementItems.add(mVideoCarItem);
		mPowerMeasurementItems.add(mVideoCarItem);
		mPowerMeasurementItems.add(mVideoCarItem);
		mPowerMeasurementItems.add(mCpuIdleItem);
		mPowerMeasurementItems.add(mSuspendItem);

		mPowerMeasurementItems.add(mShowEarphoneDialogItem);
		mPowerMeasurementItems.add(mEarphoneItem);
		mPowerMeasurementItems.add(mMusicItem);
		mPowerMeasurementItems.add(mMusicItem);

		mPowerMeasurementItems.add(mShowHDMIDialogItem);
		mPowerMeasurementItems.add(mVideoGoldenFirstTimeItem);
		mPowerMeasurementItems.add(mVideoGoldenItem);
		mPowerMeasurementItems.add(mVideoGoldenItem);
		mPowerMeasurementItems.add(mVideoCarItem);
		mPowerMeasurementItems.add(mVideoCarItem);
		mPowerMeasurementItems.add(mVideoCarItem);

		mPowerMeasurementItems.add(mShowConnectivityDialogItem);
		mPowerMeasurementItems.add(mSuspendWithConnectivityItem);

		mPowerMeasurementItems.add(mShowAirplaneDialogItem);
		mPowerMeasurementItems.add(mSuspendWithModemItem);

		mPowerMeasurementItems.add(mEndTestItem);
	}

	private void setupPendingIntent(final int position) {
		long expiredDuration = (position == 0) ? 0 : mPowerMeasurementItems
				.get(position - 1).getExpiredDuration();
		if (testType == TEST_TYPE_QUICK && expiredDuration != 0) {
			expiredDuration = DURATION_QUICK;
		}
		// emailContent = emailContent + "setupPendingIntent position=" +
		// position
		// + " expiredDuration=" + expiredDuration + " time="
		// + sdf.format(new Date(System.currentTimeMillis())) + '\n';

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {

				mHandler.post(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Log.w("Kenji",
								"Test Index="
										+ currentPosition
										+ " time="
										+ sdf.format(new Date(System
												.currentTimeMillis())));

						emailContent = emailContent
								+ "Test Index="
								+ currentPosition
								+ " time="
								+ sdf.format(new Date(System
										.currentTimeMillis())) + '\n';

						mPowerMeasurementItems.get(currentPosition).startTask();

						setupNextTask(false);
					}
				});

			}
		}, expiredDuration);
	}

	private void setupConnectivityState(boolean enabled) {
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifi.setWifiEnabled(enabled);

		if (enabled) {
			BluetoothAdapter.getDefaultAdapter().enable();
			turnGPSOn();
		} else {
			BluetoothAdapter.getDefaultAdapter().disable();
			turnGPSOff();
		}
	}

	private void turnGPSOn() {

		try {
			String provider = Settings.Secure.getString(getContentResolver(),
					Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

			if (!provider.contains("gps")) { // if gps is disabled
				final Intent poke = new Intent();
				poke.setClassName("com.android.settings",
						"com.android.settings.widget.SettingsAppWidgetProvider");
				poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
				poke.setData(Uri.parse("3"));
				sendBroadcast(poke);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	private void turnGPSOff() {
		String provider = Settings.Secure.getString(getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		if (provider.contains("gps")) { // if gps is enabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			sendBroadcast(poke);
		}
	}

	private void playCarVideo() {
		Intent intent = new Intent();
		if (gallery.equals("com.android.gallery3d")) {
			ComponentName comp = new ComponentName(gallery, gallery
					+ ".app.MovieActivity");
			intent.setComponent(comp);
		}
		intent.setAction(Intent.ACTION_VIEW);
		File file = new File(Environment.getExternalStorageDirectory()
				.getPath() + "/H264_1080p_15Mbps_30fps.mp4");
		intent.setDataAndType(Uri.fromFile(file), "video/*");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	private void playGoldenVideo() {
		Intent intent = new Intent();
		if (gallery.equals("com.android.gallery3d")) {
			ComponentName comp = new ComponentName(gallery, gallery
					+ ".app.MovieActivity");
			intent.setComponent(comp);
		}

		intent.setAction(Intent.ACTION_VIEW);
		File file = new File(Environment.getExternalStorageDirectory()
				.getPath() + "/golden_flower_h264_720_30p_7M.mp4");
		intent.setDataAndType(Uri.fromFile(file), "video/*");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

	}

	private void changeScreenRotationMode(int rotation) {
		// TODO Auto-generated method stub
		if (wm == null) {
			wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		}

		if (switchOrientationView == null) {
			switchOrientationView = new View(getApplicationContext());

			int dimension = 0;
			int pixelFormat = PixelFormat.TRANSLUCENT;
			final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
					dimension, dimension,
					WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
					WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
							| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
					pixelFormat);
			params.screenOrientation = rotation;

			wm.addView(switchOrientationView, params);
		} else {
			final WindowManager.LayoutParams params = (LayoutParams) switchOrientationView
					.getLayoutParams();
			params.screenOrientation = rotation;

			wm.updateViewLayout(switchOrientationView, params);
		}

	}

	private void turnScreenOn() {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), TurnScreenOnActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

	}

	private void killBackgroundProcess(String processName) {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

		for (RunningAppProcessInfo process : manager.getRunningAppProcesses()) {

			if (process.processName.equals(processName)) {
				// Toast.makeText(MyService.this, process.processName,
				// Toast.LENGTH_SHORT).show();

				manager.killBackgroundProcesses(processName);
			}
		}
	}

	private void playMusicTest() {
		// TODO Auto-generated method stub

		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		File file = new File(Environment.getExternalStorageDirectory()
				.getPath() + "/Music/" + "1. Bitter Heart.mp3");
		intent.setDataAndType(Uri.fromFile(file), "audio/*");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

		killBackgroundProcess("com.android.settings");

	}

	private void idleTest() {
		// TODO Auto-generated method stub

		pm = (PowerManager) getApplicationContext().getSystemService(
				Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
		wakeLock.acquire();

	}

	private void LockScreenTest() {
		// TODO Auto-generated method stub

		ComponentName componentName = new ComponentName(
				getApplicationContext(), deviceAdminReceiver.class);
		final DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		// devicePolicyManager.setMaximumTimeToLock(componentName,
		// 10000);
		boolean isAdminActive = devicePolicyManager
				.isAdminActive(componentName);
		if (!isAdminActive) {
			Intent intent = new Intent(
					DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
					componentName);
			// intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
			// "（自定義區域2）");
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		} else {
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					devicePolicyManager.lockNow(); // 鎖屏
				}
			}, 100);

		}

	}

	private void showSettingScreen() {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		ComponentName comp = new ComponentName("com.android.settings",
				"com.android.settings.Settings");
		intent.setComponent(comp);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	private void showHomeScreen() {
		// TODO Auto-generated method stub
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	@SuppressLint("NewApi")
	private void buildDialog(String title, String message) {
		// TODO Auto-generated method stub
		Builder builder = new AlertDialog.Builder(getApplicationContext(),
				AlertDialog.THEME_HOLO_LIGHT);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setNegativeButton(
				getResources().getString(R.string.service_cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						stopSelf();
					}
				});
		builder.setPositiveButton(
				getResources().getString(R.string.service_sure),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						setupNextTask(true);
					}
				});
		final AlertDialog dialog = builder.create();
		// 在dialog show方法之前添加如下代碼，表示該dialog是一個系統的dialog
		dialog.getWindow().setType(
				(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));

		dialog.show();
		dialog.setCancelable(false);

	}

	@Override
	public void onDestroy() {

		if (switchOrientationView != null) {
			wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			wm.removeView(switchOrientationView);
			switchOrientationView = null;
		}

		mHandler.removeCallbacksAndMessages(null);

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Builder builder = new AlertDialog.Builder(
						getApplicationContext(), AlertDialog.THEME_HOLO_LIGHT);
				builder.setTitle(getResources().getString(
						R.string.service_test_complete_title));
				builder.setMessage(getResources().getString(
						R.string.service_test_complete_detail));
				builder.setNeutralButton(
						getResources().getString(R.string.service_sure),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								stopSelf();

								Intent shareIntent = new Intent(
										Intent.ACTION_SEND);
								shareIntent
										.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								shareIntent.setType("text/plain");
								shareIntent.putExtra(Intent.EXTRA_EMAIL,
										"kenji_chao@asus.com");
								shareIntent.putExtra(Intent.EXTRA_SUBJECT,
										"Power apk timestamp info");
								shareIntent.putExtra(Intent.EXTRA_TEXT,
										emailContent);

								Intent new_intent = Intent.createChooser(
										shareIntent, "Share via");
								new_intent
										.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								getApplicationContext().startActivity(
										new_intent);

							}
						});
				final AlertDialog dialog = builder.create();
				// 在dialog show方法之前添加如下代碼，表示該dialog是一個系統的dialog
				dialog.getWindow().setType(
						(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));

				dialog.show();
				dialog.setCancelable(false);
			}
		});

	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	PowerMeasurementItem mShowStartDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					showHomeScreen();

					buildDialog(
							getResources().getString(
									R.string.service_start_test_title),
							getResources().getString(
									R.string.service_start_test_detail));
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mShowHomeItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					Toast.makeText(
							APMService.this,
							getResources().getString(
									R.string.service_test_started),
							Toast.LENGTH_SHORT).show();
					killBackgroundProcess("com.kenji.power");
				}
			}, DURATION_NORMAL);

	PowerMeasurementItem mShowSettingItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					showSettingScreen();
				}
			}, DURATION_NORMAL);

	PowerMeasurementItem mMusicFirstTimeItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					showHomeScreen();
					killBackgroundProcess("com.android.settings");
					LockScreenTest();
					playMusicTest();
				}
			}, DURATION_MUSIC);

	PowerMeasurementItem mMusicItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					playMusicTest();
				}
			}, DURATION_MUSIC);

	PowerMeasurementItem mVideoGoldenFirstTimeItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					turnScreenOn();

					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							showHomeScreen();
							changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
							playGoldenVideo();
						}
					}, DURATION_SMALL_DELAY);

				}
			}, DURATION_VIDEO_GOLDEN);

	PowerMeasurementItem mVideoGoldenItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					playGoldenVideo();
				}
			}, DURATION_VIDEO_GOLDEN);

	PowerMeasurementItem mVideoCarItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					playCarVideo();
				}
			}, DURATION_VIDEO_CAR);

	PowerMeasurementItem mCpuIdleItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					showHomeScreen();
					changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					idleTest();
					LockScreenTest();
				}
			}, DURATION_NORMAL);

	PowerMeasurementItem mSuspendItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					if (wakeLock != null && wakeLock.isHeld()) {
						Log.w("Kenji", "Release wakelock");
						wakeLock.release();
					}
					LockScreenTest();
				}
			}, DURATION_NORMAL);

	PowerMeasurementItem mShowEarphoneDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					showHomeScreen();
					turnScreenOn();
					buildDialog(
							getResources().getString(
									R.string.service_plug_earphone_title),
							getResources().getString(
									R.string.service_plug_earphone_detail));
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mEarphoneItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					killBackgroundProcess(gallery);
					LockScreenTest();
					playMusicTest();
				}
			}, DURATION_MUSIC);

	PowerMeasurementItem mShowHDMIDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					turnScreenOn();

					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							showHomeScreen();

							buildDialog(
									getResources().getString(
											R.string.service_plug_hdmi_title),
									getResources().getString(
											R.string.service_plug_hdmi_detail));
						}
					}, DURATION_SMALL_DELAY);

				}
			}, DURATION_ZERO);

	PowerMeasurementItem mShowConnectivityDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					showHomeScreen();
					changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

					setupConnectivityState(true);

					turnScreenOn();
					buildDialog(
							getResources().getString(
									R.string.service_connectivity_title),
							getResources().getString(
									R.string.service_connectivity_detail));
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mSuspendWithConnectivityItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub

					if (wakeLock != null && wakeLock.isHeld()) {
						Log.w("Kenji", "Release wakelock");
						wakeLock.release();
					}
					LockScreenTest();
				}
			}, DURATION_LONG);

	PowerMeasurementItem mShowAirplaneDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					setupConnectivityState(false);

					turnScreenOn();
					buildDialog(
							getResources().getString(
									R.string.service_close_airplane_title),
							getResources().getString(
									R.string.service_close_airplane_detail));
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mSuspendWithModemItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					if (wakeLock != null && wakeLock.isHeld()) {
						Log.w("Kenji", "Release wakelock");
						wakeLock.release();
					}
					LockScreenTest();
				}
			}, DURATION_LONG);

	PowerMeasurementItem mEndTestItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					turnScreenOn();

					changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

					stopSelf();

				}
			}, DURATION_ZERO);

}
