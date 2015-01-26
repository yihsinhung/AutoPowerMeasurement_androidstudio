#!/bin/sh

if [ -z "$1" ] ; then
	adboptions=""
else
	adboptions="-s $1"
fi

echo "adb install AutoPowerMeasurement_v1.6.apk..."
adb $adboptions install AutoPowerMeasurement_v1.6.apk

echo "\nadb push mediaFiles/1.\ Bitter\ Heart.mp3 storage/sdcard0/Music/..."
adb $adboptions push mediaFiles/1.\ Bitter\ Heart.mp3 storage/sdcard0/Music/

echo "\nadb push mediaFiles/golden_flower_h264_720_30p_7M.mp4 storage/sdcard0/..."
adb $adboptions push mediaFiles/golden_flower_h264_720_30p_7M.mp4 storage/sdcard0/

echo "\nadb push mediaFiles/H264_1080p_15Mbps_30fps.mp4 storage/sdcard0/..."
adb $adboptions push mediaFiles/H264_1080p_15Mbps_30fps.mp4 storage/sdcard0/
