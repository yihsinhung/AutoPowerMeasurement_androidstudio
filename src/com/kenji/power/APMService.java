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
import android.app.Notification;
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
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.NotificationCompat;
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

	public static final String ACTIVITY_PARA_ORIENTATION = "activity_para_orientation";
	private int orientation;

	private static final String ACTION_ALARM_EXPIRED = "com.kenji.power.ALARM_EXPIRED";
	public static final String ACTION_START_SERVICE = "com.kenji.power.START_SERVICE";
	public static final String ACTION_STOP_SERVICE = "com.kenji.power.STOP_SERVICE";
	private static final String ALARM_EXPIRED_POSITION = "alarm_expired_position";

	private static final long DURATION_ZERO = 0;
	private static final long DURATION_NORMAL = 490000;
	private static final long DURATION_MUSIC = 160000;
	private static final long DURATION_VIDEO_GOLDEN = 110000;
	private static final long DURATION_VIDEO_CAR = 35000;
	private static final long DURATION_LONG = 360000;

	private static final long DURATION_SMALL_DELAY = 500;

	private static final long DURATION_QUICK = 5000;

	private static final long DURATION_COUNDOWN_TIMER_ALL = 30000;
	private static final long DURATION_COUNDOWN_TIMER_TICK = 1000;

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
	private boolean isGPSFromHighToLow;
	private boolean isGPSFirstFix;
	private CountDownTimer mCountDownTimer = new CountDownTimer(
			DURATION_COUNDOWN_TIMER_ALL, DURATION_COUNDOWN_TIMER_TICK) {

		@Override
		public void onTick(long millisUntilFinished) {
			int secondsLeft = (int) millisUntilFinished / 1000;
			String searchProgressText = getResources().getString(
					R.string.service_gps_searching);
			if (mSearchingGPSProgressDialog != null
					&& mSearchingGPSProgressDialog.isShowing()) {
				mSearchingGPSProgressDialog.setMessage(searchProgressText
						.replace("#seconds#", String.valueOf(secondsLeft)));
			}
		}

		@Override
		public void onFinish() {
			finishGPSFirstFix();
		}
	};

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
				} else if (!isGpsEnabled(context) && !isGPSFromHighToLow) {
					isGPSFromHighToLow = true;
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
			Log.d(MainActivity.TAG, "提醒視窗跳出");
			emailContent = emailContent + "提醒視窗跳出" + " time="
					+ sdf.format(new Date(System.currentTimeMillis())) + '\n';
		}
	}

	private ArrayList<PowerMeasurementItem> mPowerMeasurementItems = new ArrayList<PowerMeasurementItem>();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent.getAction().equals(ACTION_STOP_SERVICE)) {
			stopSelf();
		} else {
			foregroundServiceSettings(startId);

			gallery = intent.getExtras().getString("gallery", "");
			testType = intent.getExtras().getInt(ACTIVITY_PARA_TEST_TYPE,
					TEST_TYPE_QUICK);
			orientation = intent.getExtras().getInt(ACTIVITY_PARA_ORIENTATION,
					Configuration.ORIENTATION_PORTRAIT);
			Log.d(MainActivity.TAG, "testType=" + testType);
		}

		return Service.START_NOT_STICKY;
	}

	private void foregroundServiceSettings(int startId) {
		// Notification item click intent
		Bitmap icon = BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_launcher);
		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		// Notification stop button click intent
		Intent stopIntent = new Intent(this, APMService.class);
		stopIntent.setAction(ACTION_STOP_SERVICE);
		PendingIntent pendingstopIntent = PendingIntent.getService(this, 0,
				stopIntent, 0);

		Notification notification = new NotificationCompat.Builder(this)
				.setContentTitle(getResources().getString(R.string.app_name))
				.setTicker(getResources().getString(R.string.app_name))
				.setContentText(
						getResources().getString(R.string.service_noti_content))
				.setSmallIcon(R.drawable.ic_launcher)
				.setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
				.setContentIntent(pendingIntent)
				.setOngoing(true)
				.addAction(android.R.drawable.ic_media_pause,
						getResources().getString(R.string.service_noti_stop),
						pendingstopIntent).build();

		startForeground(startId, notification);
	}

	@Override
	public void onCreate() {
		initVariables();
		registerAlarmReceiver();
		setupMeasureItems();

		setupPendingIntent(currentPosition);

	}

	private void initVariables() {
		currentPosition = 0;
		isGPSFromLowToHigh = false;
		isGPSFromHighToLow = false;
		isGPSFirstFix = false;
	}

	private void registerAlarmReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_ALARM_EXPIRED);
		filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
		registerReceiver(alarmExpiredReceiver, filter);
	}

	private void setupMeasureItems() {
		// normal test items
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

		// audio playback with earphone plug in
		mPowerMeasurementItems.add(mShowEarphoneDialogItem);
		mPowerMeasurementItems.add(mEarphoneItem);
		mPowerMeasurementItems.add(mMusicItem);
		mPowerMeasurementItems.add(mMusicItem);

		// video playback with HDMI connected
		mPowerMeasurementItems.add(mShowHDMIDialogItem);
		mPowerMeasurementItems.add(mVideoGoldenFirstTimeItem);
		mPowerMeasurementItems.add(mVideoGoldenItem);
		mPowerMeasurementItems.add(mVideoGoldenItem);
		mPowerMeasurementItems.add(mVideoCarItem);
		mPowerMeasurementItems.add(mVideoCarItem);
		mPowerMeasurementItems.add(mVideoCarItem);

		// suspend with connectivity
		mPowerMeasurementItems.add(mTurnGPSOnDialogItem);
		mPowerMeasurementItems.add(mSearchingGPSDialogItem);
		mPowerMeasurementItems.add(mShowConnectivityDialogItem);
		mPowerMeasurementItems.add(mSuspendLongItem);

		// suspend with modem on
		mPowerMeasurementItems.add(mTurnGPSOffDialogItem);
		mPowerMeasurementItems.add(mTurnAirplaneOffDialogItem);
		mPowerMeasurementItems.add(mSuspendLongItem);

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
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			if (wm == null) {
				wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			}

			if (switchOrientationView == null) {
				switchOrientationView = new View(getApplicationContext());

				int dimension = 0;
				int pixelFormat = PixelFormat.TRANSLUCENT;
				final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
						dimension,
						dimension,
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

	private void buildGPSDialog(String title, String message,
			final boolean isTurnGpsOn) {
		Builder builder = new AlertDialog.Builder(getApplicationContext(),
				AlertDialog.THEME_HOLO_LIGHT);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setNeutralButton(
				getResources().getString(R.string.service_gps_setting),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if ((isTurnGpsOn && !isGpsEnabled(getApplicationContext()))
								|| (!isTurnGpsOn && isGpsEnabled(getApplicationContext()))) {
							Intent intent = new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
						} else {
							Toast.makeText(
									APMService.this,
									isTurnGpsOn ? getResources().getString(
											R.string.service_gps_already_on)
											: getResources()
													.getString(
															R.string.service_gps_already_off),
									Toast.LENGTH_SHORT).show();
							setupNextTask(true);
						}

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

	private void setGpsFirstFix() {
		mSearchingGPSProgressDialog = new ProgressDialog(
				getApplicationContext(), AlertDialog.THEME_HOLO_LIGHT);
		mSearchingGPSProgressDialog.getWindow().setType(
				(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
		mSearchingGPSProgressDialog.setCancelable(false);
		String searchProgressText = getResources().getString(
				R.string.service_gps_searching);
		mSearchingGPSProgressDialog.setMessage(searchProgressText.replace(
				"#seconds#",
				String.valueOf((int) DURATION_COUNDOWN_TIMER_ALL / 1000)));
		mSearchingGPSProgressDialog.show();

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mCountDownTimer.start();
			}
		}, DURATION_COUNDOWN_TIMER_TICK);

		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				1000, 10, APMService.this);
		mLocationManager.addGpsStatusListener(APMService.this);
	}

	private void finishGPSFirstFix() {
		if (!isGPSFirstFix) {
			isGPSFirstFix = true;
			if (mSearchingGPSProgressDialog != null
					&& mSearchingGPSProgressDialog.isShowing()) {
				mSearchingGPSProgressDialog.dismiss();
			}

			if (mCountDownTimer != null) {
				mCountDownTimer.cancel();
			}

			if (mLocationManager != null) {
				mLocationManager.removeGpsStatusListener(APMService.this);
				mLocationManager.removeUpdates(APMService.this);
			}
			setupNextTask(true);
		}
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		unregisterReceiver(alarmExpiredReceiver);

		if (switchOrientationView != null) {
			wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			wm.removeView(switchOrientationView);
			switchOrientationView = null;
		}

		if (mCountDownTimer != null) {
			mCountDownTimer.cancel();
			mCountDownTimer = null;
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
										shareIntent,
										getResources().getString(
												R.string.service_record_log));
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
					killBackgroundProcess(gallery);
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
						Log.d(MainActivity.TAG, "Release wakelock");
						wakeLock.release();
					}
					LockScreenTest();
				}
			}, DURATION_NORMAL);

	PowerMeasurementItem mShowEarphoneDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
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

	PowerMeasurementItem mTurnGPSOnDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					showHomeScreen();
					changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

					setupConnectivityState(true);

					buildGPSDialog(
							getResources().getString(
									R.string.service_turn_gps_on_title),
							getResources().getString(
									R.string.service_turn_gps_on_detail), true);
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mSearchingGPSDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					showHomeScreen();
					setGpsFirstFix();
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mShowConnectivityDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					buildDialog(
							getResources().getString(
									R.string.service_connectivity_title),
							getResources().getString(
									R.string.service_connectivity_detail));
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mSuspendLongItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					if (wakeLock != null && wakeLock.isHeld()) {
						Log.d(MainActivity.TAG, "Release wakelock");
						wakeLock.release();
					}
					LockScreenTest();
				}
			}, DURATION_LONG);

	PowerMeasurementItem mTurnGPSOffDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					setupConnectivityState(false);
					turnScreenOn();
					buildGPSDialog(
							getResources().getString(
									R.string.service_turn_gps_off_title),
							getResources().getString(
									R.string.service_turn_gps_off_detail),
							false);
				}
			}, DURATION_ZERO);

	PowerMeasurementItem mTurnAirplaneOffDialogItem = new PowerMeasurementItem(
			new PowerMeasurementItem.OnAlarmReceivedListener() {

				@Override
				public void onStart() {
					showHomeScreen();
					buildDialog(
							getResources().getString(
									R.string.service_turn_airplane_off_title),
							getResources().getString(
									R.string.service_turn_airplane_off_detail));
				}
			}, DURATION_ZERO);

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
			Log.d(MainActivity.TAG, "GPS_EVENT_FIRST_FIX");
			finishGPSFirstFix();
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
