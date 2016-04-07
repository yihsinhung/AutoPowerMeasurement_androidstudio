package com.kenji.power;

public class PowerMeasurementItem {

	private OnAlarmReceivedListener onAlarmReceivedListener;
	
	// Execution time of this item
	private long expiredDuration;

	public PowerMeasurementItem(OnAlarmReceivedListener listener, long duration) {
		this.onAlarmReceivedListener = listener;
		this.expiredDuration = duration; 
	}

	public interface OnAlarmReceivedListener {
		void onStart();
	}

	public void startTask() {
		onAlarmReceivedListener.onStart();
	}

	public long getExpiredDuration() {
		return expiredDuration;
	}
	
}
