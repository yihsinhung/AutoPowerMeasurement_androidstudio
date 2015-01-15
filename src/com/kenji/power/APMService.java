package com.kenji.power;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.KeyguardManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

@SuppressLint("InlinedApi")
public class APMService extends Service {

	Handler handler = new Handler();
	Handler handler2 = new Handler();
	// private static final long CONST_DELAYED_TIME = 3000;
	private static long CONST_DELAYED_TIME = 100000;

	PowerManager pm;
	WakeLock wakeLock;

	View view;
	WindowManager wm;

	String gallery = "";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO do something useful
		Log.w("Kenji", intent.getExtras().getString("gallery"));
		gallery = intent.getExtras().getString("gallery", "");
		CONST_DELAYED_TIME = intent.getExtras().getLong("time", 100000);
		return Service.START_NOT_STICKY;
	}

	@Override
	public void onCreate() {


		handler.post(new Runnable() {

			@Override
			public void run() {
				HomeTest();

				buildDialogFirst(
						getResources().getString(
								R.string.service_start_test_title),
						getResources().getString(
								R.string.service_start_test_detail));

			}
		});

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

		wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		if (view != null) {
			wm.removeView(view);
			view = null;
		}

		view = new View(getApplicationContext());
		int dimension = 0;
		int pixelFormat = PixelFormat.TRANSLUCENT;
		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				dimension, dimension,
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
						| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				pixelFormat);
		params.screenOrientation = rotation;

		wm.addView(view, params);

	}

	private void turnScreenOn() {
		// TODO Auto-generated method stub
		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		final KeyguardManager.KeyguardLock kl = km
				.newKeyguardLock("MyKeyguardLock");
		kl.disableKeyguard();

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
		wakeLock.acquire();
		handler2.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				wakeLock.release();
			}
		}, 100);

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

		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		final KeyguardManager.KeyguardLock kl = km
				.newKeyguardLock("MyKeyguardLock");
		kl.disableKeyguard();

		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		File file = new File(Environment.getExternalStorageDirectory()
				.getPath() + "/Music/" + "1. Bitter Heart.mp3");
		intent.setDataAndType(Uri.fromFile(file), "audio/*");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

		killBackgroundProcess("com.android.settings");

		kl.reenableKeyguard();

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
			handler2.postDelayed(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					devicePolicyManager.lockNow(); // 鎖屏
				}
			}, 100);

		}

	}

	private void SettingsTest() {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		ComponentName comp = new ComponentName("com.android.settings",
				"com.android.settings.Settings");
		intent.setComponent(comp);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

	}

	private void HomeTest() {
		// TODO Auto-generated method stub
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

	}

	@SuppressLint("NewApi")
	private void buildDialogFirst(String title, String message) {
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
						handler.post(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								Toast.makeText(
										APMService.this,
										getResources().getString(
												R.string.service_test_started),
										Toast.LENGTH_SHORT).show();
								killBackgroundProcess("com.kenji.power");

							}
						});

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								SettingsTest();

							}
						}, (long) (CONST_DELAYED_TIME * 4.9));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								HomeTest();
								killBackgroundProcess("com.android.settings");

								LockScreenTest();
								playMusicTest();

							}
						}, (long) (CONST_DELAYED_TIME * 9.8));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub

								playMusicTest();
							}
						}, (long) (CONST_DELAYED_TIME * 11.4));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playMusicTest();

							}
						}, (long) (CONST_DELAYED_TIME * 13));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								// playGoldenVideoTest();
								// stopMusic();

								turnScreenOn();

								handler2.postDelayed(new Runnable() {
									public void run() {
										HomeTest();
										changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
										playGoldenVideo();
									}
								}, 100);

							}
						}, (long) (CONST_DELAYED_TIME * 14.6));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playGoldenVideo();
							}
						}, (long) (CONST_DELAYED_TIME * 15.7));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playGoldenVideo();
							}
						}, (long) (CONST_DELAYED_TIME * 16.8));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playCarVideo();
							}
						}, (long) (CONST_DELAYED_TIME * 17.9));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playCarVideo();
							}
						}, (long) (CONST_DELAYED_TIME * 18.25));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playCarVideo();
							}
						}, (long) (CONST_DELAYED_TIME * 18.6));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								HomeTest();
								changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
								idleTest();
								LockScreenTest();
							}
						}, (long) (CONST_DELAYED_TIME * 18.95));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub

								wakeLock.release();
								// turnScreenOn();

								// Intent intent = new Intent();
								// intent.setClass(getApplicationContext(),
								// UpdateService.class);
								// getApplicationContext().startService(intent);

								LockScreenTest();
							}
						}, (long) (CONST_DELAYED_TIME * 24.25));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								HomeTest();
								turnScreenOn();
								// changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
								buildDialog(
										getResources()
												.getString(
														R.string.service_plug_earphone_title),
										getResources()
												.getString(
														R.string.service_plug_earphone_detail));
							}
						}, (long) (CONST_DELAYED_TIME * 24.55));
					}
				});
		final AlertDialog dialog = builder.create();
		// 在dialog show方法之前添加如下代碼，表示該dialog是一個系統的dialog
		dialog.getWindow().setType(
				(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));

		dialog.show();
		dialog.setCancelable(false);
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
						handler.post(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								killBackgroundProcess(gallery);
								LockScreenTest();
								playMusicTest();
							}
						});

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playMusicTest();
							}
						}, (long) (CONST_DELAYED_TIME * 1.6));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playMusicTest();

							}
						}, (long) (CONST_DELAYED_TIME * 3.2));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								turnScreenOn();

								handler2.postDelayed(new Runnable() {
									public void run() {
										HomeTest();
										buildHDMIDialog(
												getResources()
														.getString(
																R.string.service_plug_hdmi_title),
												getResources()
														.getString(
																R.string.service_plug_hdmi_detail));
									}
								}, 100);

							}
						}, (long) (CONST_DELAYED_TIME * 4.8));
					}
				});
		final AlertDialog dialog = builder.create();
		// 在dialog show方法之前添加如下代碼，表示該dialog是一個系統的dialog
		dialog.getWindow().setType(
				(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));

		dialog.show();
		dialog.setCancelable(false);
		// handler.postDelayed(new Runnable() {
		// @Override
		// public void run() {
		//
		// dialog.show();
		// }
		// }, CONST_DELAYED_TIME);
	}

	@SuppressLint("NewApi")
	private void buildHDMIDialog(String title, String message) {
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
						handler.post(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
								playGoldenVideo();
							}
						});

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playGoldenVideo();
							}
						}, (long) (CONST_DELAYED_TIME * 1.1));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playGoldenVideo();
							}
						}, (long) (CONST_DELAYED_TIME * 2.2));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playCarVideo();
							}
						}, (long) (CONST_DELAYED_TIME * 3.3));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playCarVideo();
							}
						}, (long) (CONST_DELAYED_TIME * 3.65));

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								playCarVideo();
							}
						}, CONST_DELAYED_TIME * 4);

						handler.postDelayed(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								HomeTest();
								stopSelf();
								changeScreenRotationMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
								// Toast.makeText(getApplicationContext(),
								// "測試項目已全部結束!",
								// Toast.LENGTH_SHORT).show();
							}
						}, (long) (CONST_DELAYED_TIME * 4.35));
					}
				});
		final AlertDialog dialog = builder.create();
		// 在dialog show方法之前添加如下代碼，表示該dialog是一個系統的dialog
		dialog.getWindow().setType(
				(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));

		dialog.show();
		dialog.setCancelable(false);
		// handler.postDelayed(new Runnable() {
		// @Override
		// public void run() {
		//
		// dialog.show();
		// }
		// }, CONST_DELAYED_TIME);
	}

	// @Override
	// public void onStart(Intent intent, int startId) {
	// super.onStart(intent, startId);
	// }

	@Override
	public void onDestroy() {

		if (view != null) {
			// Toast.makeText(getApplicationContext(), "Removed",
			// Toast.LENGTH_SHORT).show();
			wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			wm.removeView(view);
			view = null;
		}

		handler.removeCallbacksAndMessages(null);

		handler.post(new Runnable() {
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

}
