# Auto Power Measurement Tool
此自動化量測工具是為了增進測量 Power 的效率，需搭配 Host 端的 Excel with VBA tool 以及 Device 端的 app 一起使用，達到自動化量測的目的。


## Features
* 加入 Suspend with Connectivity (Bluetooh, Wifi, GPS) 以及 Suspend with modem on 的測項
* 使用 alarm manager + broadcast receiver 來實作延時任務，在suspend的時候執行任務的時間不會跑掉
* 將所有測項模組化，方便未來新增或刪減測項
* 在預設直的螢幕(ME581CL) 跟橫的螢幕(TF303K) 測試過皆可以完美運行
* 優化初始設定的提示，讓第一次使用的人可以輕鬆上手


## All Test Items and Measure Orders
1. idle (show home screen)
2. idle (show setting screen)
3. mp3 playback (normal)
4. 720p video playback
5. 1080p video playback
6. idle (screen off)
7. suspend
8. mp3 playback (headset)
9. 720p video playback with HDMI
10. 1080p video playback with HDMI
11. suspend with WIFI + BT + GPS on
12. suspend with Camp to live network + Data OFF

## Required files when testing
* Release/PowerMeasurement_v1.6.apk
* Release/AutoPowerMeasurement_VBA_v1.6.xlsm


## Initial Settings
1. PC 端請安裝 Agilent 14565B tool，並取消詢問儲存waveform的功能。如下圖:
 
2. Device 的 power 測量環境請先設定完成。
3. Audio 與 Video 測項所需之檔案的檔名及放入機台的位置(務必相同): 
  * golden_flower_h264_720_30p_7M.mp4 (位置: \sdcard\)
  * H264_1080p_15Mbps_30fps.mp4       (位置: \sdcard\)
  * 1. Bitter Heart.mp3               (位置: \sdcard\music\)

## Host Excel Tool Usage
PC端Excel with VBA tool功能介紹:
 
自動量測SOP(需搭配PowerMeasurement_20150122.apk):
1.	開啟AutoPowerMeasurement_VBA Excel 檔案。
2.	點選按鈕，會開啟Agilent 14565B automation interface並建立與Excel的連結。
注意: 請利用Excel 按鈕來自動開啟agilent tool，若手動從host電腦啟動Agilent 14565B tool會有測量結果儲存上的問題。
3.	此版自動量測excel開啟agilent後會自動設定輸出電壓為4v，並turn on output。
注意: 開啟的agilent automation interface有顯示上的bug，測量前請先從Agilent power supply 的面版確認電壓4v 有輸出。
4.	點選按鈕會彈出一提示對話框，下一步同步按下device apk秀出之提示對話框確定鈕即開始測量。


## Device App Usage
###安裝方式
將裝置經由USB連到電腦，下指令
adb install AutoPowerMeasurement.apk 來安裝此app

###首次使用
使用前請自行做好機台的設定，並將「golden_flower_h264_720_30p_7M.mp4」以及「H264_1080p_15Mbps_30fps.mp4」這兩個影片檔放在儲存裝置的根目錄下（例如: /sdcard），並將「1. Bitter Heart.mp3」放置在Music資料夾下（例如: /sdcard/Music）
第一次使用建議先點選「快速測試」來判斷裝置是否可使用本 App 完成所有測項，由於本系統會使用到鎖屏的權限來讓裝置進入vdd_min的狀態，所以第一次測試前會要求使用者開啟權限，如下圖。

###開始完整測試
點選「開始完整測試以後」，App會自動關閉回到Home，並且跳出提示對話框如下圖，
下一步務必同時按下PC端與裝置端之確定鈕才能正確測量。
 
接著idle, display home screen 與 idle, display setting screen畫面分別如下圖，
量完Home跟Setting的測項以後，系統會自己關上螢幕並且播放音樂，接著依序播放黃金甲以及賽車的影片，如下圖所示
影片測試結束後，系統會進入idle (display off) 的狀態，此時持有wakelock，螢幕關閉但CPU仍間歇性被喚醒，經過一段時間後會解鎖wakelock，進入vdd_min的狀態，這兩個測項結束以後，根據Excel上面的提示按下裝置的電源鍵，過一段時間後會出現插入耳機的提示，一樣同時按下電腦與裝置端的確定按鈕開始進行測量。
耳機的測項結束後會跳出對話框提示要連接HDMI線，接上線以後一樣同時按下電腦與裝置端的確定按鈕開始進行測量，影片結束後會跳出對話框提示測量已經全部結束！

###移除apk
由於此app有使用到裝置管理員的權限，沒有辦法直接刪除，必須到Settings > Security > Device administrators 裡頭將Auto Power Measurement取消勾選，接著才可以移除本App

