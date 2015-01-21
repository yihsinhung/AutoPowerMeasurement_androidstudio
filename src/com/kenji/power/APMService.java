package com.kenji.power;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

@SuppressLint("InlinedApi")
public class APMService extends Service implements LocationListener,
		GpsStatus.Listener {

	public static final String ACTIVITY_PARA_TEST_TYPE = "activity_para_test_type";
	public static final int TEST_TYPE_QUICK = 0;
	public static final int TEST_TYPE_FULL = 1;
	public static final int TEST_TYPE_SINGLE = 2;
	private int testType;

	private static final String ACTION_ALARM_EXPIRED = "com.kenji.power.ALARM_EXPIRED";
	private static final String ALARM_EXPIRED_POSITION = "alarm_expired_position";

	private static final long DURATION_ZERO = 0;
	private static final long DURATION_NORMAL = 490000;
	private static final long DURATION_MUSIC = 160000;
	private static final long DURATION_VIDEO_GOLDEN = 110000;
	private static final long DURATION_VIDEO_CAR = 35000;
	private static final long DURATION_LONG = 360000;

	private static final long DURATION_SMALL_DELAY = 500;

	private static final long DURATION_QUICK = 5000;

	Handler mHandler = new Handler();
	ProgressDialog mSearchingGPSProgressDialog;
	LocationManager mLocationManager;

	PowerManager pm;
	WakeLock wakeLock;

	View switchOrientationView;
	WindowManager wm;

	String gallery = "";

	private int currentPosition;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private String emailContent = "";

	public static final String VIDEO_GOLDEN_FILE_NAME = "golden_flower_h264_720_30p_7M.mp4";
	public static final String VIDEO_CAR_FILE_NAME = "H264_1080p_15Mbps_30fps.mp4";

	private boolean isGPSFromLowToHigh;

	private final BroadcastReceiver alarmExpiredReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String intentAction = intent.getAction();

			if (intentAction.equals(ACTION_ALARM_EXPIRED)) {
				currentPosition = intent.getExtras().getInt(
						ALARM_EXPIRED_POSITION);
				Log.d(MainActivity.TAG,
						"Test Index="
								+ currentPosition
								+ " time="
								+ sdf.format(new Date(System
										.currentTimeMillis())));

				emailContent = emailContent + "Test Index=" + currentPosition
						+ " time="
						+ sdf.format(new Date(System.currentTimeMillis()))
						+ '\n';

				mPowerMeasurementItems.get(currentPosition).startTask();

				setupNextTask(false);
			} else if (intentAction
					.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {

				if (isGpsEnabled(context) && !isGPSFromLowToHigh) {
					isGPSFromLowToHigh = true;
					setupNextTask(true);
				}
			}
		}
	};

	private void setupNextTask(boolean forceExecute) {
		if (currentPosition + 1 < mPowerMeasurementItems.size()
				&& (mPowerMeasurementItems.get(currentPosition)
						.getExpiredDuration() != DURATION_ZERO) || forceExecute) {
			currentPosition++;
			setupPendingIntent(currentPosition);
		} else {
			Log.w(MainActivity.TAG, "提醒視窗跳出");
			emailContent = emailContent + "提醒視窗跳出" + " time="
					+ sdf.format(new Date(System.currentTimeMillis())) + '\n';
		}
	}

	private ArrayList<PowerMeasurementItem> mPowerMeasurementItems = new ArrayList<PowerMeasurementItem>();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		gallery = intent.getExtras().getString("gallery", "");
		testType = intent.getExtras().getInt(ACTIVITY_PARA_TEST_TYPE,
				TEST_TYPE_QUICK);
		Log.d(MainActivity.TAG, "testType=" + testType);

		return Service.START_NOT_STICKY;
	}

	@Override
	public void onCreate() {

		registerAlarmReceiver();
		setupMeasureItems();

		currentPosition = 0;
		isGPSFromLowToHigh = false;

		setupPendingIntent(currentPosition);

	}

	private void registerAlarmReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_ALARM_EXPIRED);
		filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
		registerReceiver(alarmExpiredReceiver, filter);
	}

	private void setupMeasureItems() {
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

		mPowerMeasurementItems.add(mShowGPSDialogItem);
		mPowerMeasurementItems.add(mSearchingGPSDialogItem);
		mPowerMeasurementItems.add(mShowConnectivityDialogItem);
		mPowerMeasurementItems.add(mSuspendWithConnectivityItem);

		mPowerMeasurementItems.add(mShowAirplaneDialogItem);
		mPowerMeasurementItems.add(mSuspendWithModemItem);

		mPowerMeasurementItems.add(mEndTestItem);
	}

	private void setupPendingIntent(int position) {
		Calendar cal = Calendar.getInstance();
		int expiredDuration = (position == 0) ? 0
				: (int) mPowerMeasurementItems.get(position - 1)
						.getExpiredDuration();
		if (testType == TEST_TYPE_QUICK && expiredDuration != 0) {
			expiredDuration = (int) DURATION_QUICK;
		}

		Log.d(MainActivity.TAG,
				"setupPendingIntent position=" + position + " expiredDuration="
						+ expiredDuration + " time="
						+ sdf.format(new Date(System.currentTimeMillis())));
		// emailContent = emailContent + "setupPendingIntent position=" +
		// position
		// + " expiredDuration=" + expiredDuration + " time="
		// + sdf.format(new Date(System.currentTimeMillis())) + '\n';
		cal.add(Calendar.MILLISECOND, expiredDuration);

		Intent intent = new Intent();
		intent.setAction(ACTION_ALARM_EXPIRED);
		Bundle bundle = new Bundle();
		bundle.putInt(ALARM_EXPIRED_POSITION, position);
		intent.putExtras(bundle);

		int requestCode = (int) System.currentTimeMillis();
		PendingIntent pi = PendingIntent.getBroadcast(this, requestCode,
				intent, PendingIntent.FLAG_ONE_SHOT);

		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
	}

	private void setupConnectivityState(boolean enabled) {
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifi.setWifiEnabled(enabled);

		if (enabled) {
			BluetoothAdapter.getDefaultAdapter().enable();
		} else {
			BluetoothAdapter.getDefaultAdapter().disable();
		}
	}

	private void playVideo(String filename) {
		Intent intent = new Intent();
		if (gallery.equals("com.android.gallery3d")) {
			ComponentName comp = new ComponentName(gallery, gallery
					+ ".app.MovieActivity");
			intent.setComponent(comp);
		}

		intent.setAction(Intent.ACTION_VIEW);
		File fileVideo = new File(Environment.getExternalStorageDirectory()
				.getPath() + "/" + filename);
		intent.setDataAndType(Uri.fromFile(fileVideo), "video/*");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

	}

	private void changeScreenRotationMode(int rotation) {
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
		pm = (PowerManager) getApplicationContext().getSystemService(
				Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
		wakeLock.acquire();

	}

	private void LockScreenTest() {
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
					devicePolicyManager.lockNow(); // 鎖屏
				}
			}, 100);

		}

	}

	private void showSettingScreen() {
		Intent intent = new Intent();
		ComponentName comp = new ComponentName("com.android.settings",
				"com.android.settings.Settings");
		intent.setComponent(comp);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	private void showHomeScreen() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	private void playSystemNotificationSound() {
		try {
			Uri notification = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
					notification);
			r.play();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressLint("NewApi")
	private void buildDialog(String title, String message) {
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

		playSystemNotificationSound();

	}

	private void buildGPSDialog(String title, String message) {
		Builder builder = new AlertDialog.Builder(getApplicationContext(),
				AlertDialog.THEME_HOLO_LIGHT);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setNeutralButton(
				getResources().getString(R.string.service_gps_setting),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(
								Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
					}
				});
		final AlertDialog dialog = builder.create();
		// 在dialog show方法之前添加如下代碼，表示該dialog是一個系統的dialog
		dialog.getWindow().setType(
				(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));

		dialog.show();
		dialog.setCancelable(false);

		playSystemNotificationSound();

	}

	public static boolean isGpsEnabled(Context context) {

		final int locationMode;
		try {
			locationMode = Secure.getInt(context.getContentResolver(),
					Secure.LOCATION_MODE);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		switch (locationMode) {

		case Secure.LOCATION_MODE_HIGH_ACCURACY:
		case Secure.LOCATION_MODE_SENSORS_ONLY:
			return true;
		case Secure.LOCATION_MODE_BATTERY_SAVING:
		case Secure.LOCATION_MODE_OFF:
		default:
			return false;
		}
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(alarmExpiredReceiver);

		if (mLocationManager != null) {
			mLocationManager.removeGpsStatusListener(APMService.this);
			mLocationManager.removeUpdates(APMService.this);
			Toast.makeText(APMService.this, "Remove location update",
					Toast.LENGTH_SHORT).show();
		}

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
										shareIntent, "Recotd Timestamp");
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

				playSystemNotificationSound();
			}
		});

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	PowerMeasurementItem mShowStartDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
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
					showSettingScreen();
				}
			}, DURATION_NORMAL);

	PowerMeasurementItem mMusicFirstTimeItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
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
					playMusicTest();
				}
			}, DURATION_MUSIC);

	PowerMeasurementItem mVideoGoldenFirstTimeItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					turnScreenOn();

					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							showHomeScreen();
							changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
							playVideo(VIDEO_GOLDEN_FILE_NAME);
						}
					}, DURATION_SMALL_DELAY);

				}
			}, DURATION_VIDEO_GOLDEN);

	PowerMeasurementItem mVideoGoldenItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					playVideo(VIDEO_GOLDEN_FILE_NAME);
				}
			}, DURATION_VIDEO_GOLDEN);

	PowerMeasurementItem mVideoCarItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					playVideo(VIDEO_CAR_FILE_NAME);
				}
			}, DURATION_VIDEO_CAR);

	PowerMeasurementItem mCpuIdleItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
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
					killBackgroundProcess(gallery);
					LockScreenTest();
					playMusicTest();
				}
			}, DURATION_MUSIC);

	PowerMeasurementItem mShowHDMIDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					turnScreenOn();

					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
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

	PowerMeasurementItem mShowGPSDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					showHomeScreen();
					changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

					setupConnectivityState(true);

					buildGPSDialog(
							getResources()
									.getString(R.string.service_gps_title),
							getResources().getString(
									R.string.service_gps_detail));
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mSearchingGPSDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							showHomeScreen();
							mSearchingGPSProgressDialog = new ProgressDialog(
									getApplicationContext(),
									AlertDialog.THEME_HOLO_LIGHT);
							mSearchingGPSProgressDialog
									.getWindow()
									.setType(
											(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
							mSearchingGPSProgressDialog.setCancelable(false);
							mSearchingGPSProgressDialog
									.setMessage(getResources().getString(
											R.string.service_gps_searching));
							mSearchingGPSProgressDialog.show();

							mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
							mLocationManager.requestLocationUpdates(
									LocationManager.GPS_PROVIDER, 1000, 10,
									APMService.this);
							mLocationManager
									.addGpsStatusListener(APMService.this);

						}
					}, DURATION_SMALL_DELAY);
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mShowConnectivityDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
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
					turnScreenOn();
					changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					stopSelf();
				}
			}, DURATION_ZERO);

	@Override
	public void onGpsStatusChanged(int event) {
		switch (event) {
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			Log.w(MainActivity.TAG, "GPS_EVENT_FIRST_FIX");
			if (mSearchingGPSProgressDialog != null
					&& mSearchingGPSProgressDialog.isShowing()) {
				mSearchingGPSProgressDialog.dismiss();
			}
			break;
		}
	}

	@Override
	public void onLocationChanged(final Location location) {
		Log.w(MainActivity.TAG, "onLocationChanged");

	}

	@Override
	public void onProviderDisabled(String arg0) {
		Log.w(MainActivity.TAG, "onProviderDisabled");

	}

	@Override
	public void onProviderEnabled(String arg0) {
		Log.w(MainActivity.TAG, "onProviderEnabled");

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.w(MainActivity.TAG, "onStatusChanged provider = " + provider
				+ " status=" + status + " extras=" + extras.toString());

	}

}
