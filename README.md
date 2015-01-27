# Auto Power Measurement Tool
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/ic_launcher.png" width="200px" height="200px" />

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
* Release/AutoPowerMeasurement_v1.6.apk
* Release/AutoPowerMeasurement_VBA_v1.6.xlsm


## Initial Settings
PC 端請安裝 Agilent 14565B tool，並取消詢問儲存 waveform 的功能。如下圖:
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/excel/excel_01.png" width="400px" height="362px" />


### 機台初始設定
1. 將「golden_flower_h264_720_30p_7M.mp4」以及「H264_1080p_15Mbps_30fps.mp4」這兩個影片檔放在儲存裝置的根目錄下（例如: /sdcard），並將「1. Bitter Heart.mp3」放置在Music資料夾下（例如: /sdcard/Music)
P.S. Linux User 可使用 Release/setupAutoPowerMeasurement.sh 直接將 mediaFiles 推到機台並安裝 AutoPowerMeasurement_v1.6.apk
2. (Optional) 3G 或 LTE 機台請插入 sim 卡，並確認有基地台訊號後 disable network data
3. Date &amp; time > Select time zone 設定為 GMT +8，並連上一個可用的  Wifi 取得現在時間 
4. Display > Brightness 設定為最亮，並取消 Automatic brightness
5. Display > Sleep 設定為 Never
6. 開啟 Airplane mode


## PC 端 Excel with VBA tool 功能介紹
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/excel/excel_02.jpg" width="400px" height="435px" />


###自動量測步驟
1.	開啟AutoPowerMeasurement_VBA Excel 檔案。
2.	點選按鈕2，會開啟 Agilent 14565B automation interface 並建立與 Excel 的連結。
注意: 請利用 Excel 按鈕2來自動開啟 agilent tool，若手動從 host 電腦啟動 Agilent 14565B tool 會有測量結果儲存上的問題。
3.	此版自動量測 Excel 開啟 agilent 後會自動設定輸出電壓為 4v，並 turn on output。
注意: 開啟的 agilent automation interface 有顯示上的bug，測量前請先從 Agilent power supply 的面版確認電壓4v 有輸出。
4.	點選按鈕5會彈出一提示對話框，下一步同步按下device apk秀出之提示對話框確定鈕即開始測量。
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/excel/excel_03.png" width="265px" height="178px" />
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/excel/excel_04.png" width="487px" height="178px" />

5. 在測項6, 9, 11, 12測量前excel與device apk 皆會出現提示對話框提醒使用者完成測項設定，完成後務必同時按下兩端之確定按鈕才能繼續正常測量。
6. 測項11與12測量完成後會自動儲存 data logging binary 到與 Excel 同一個資料夾。
如果測量結果有疑慮，可另外開啟 agilent 14565B tool 再開啟相關 bin 分析。
7. 各測項測量完成都會自動帶入結果並計算 avg. 到 Excel 並出現提示訊息提醒使用者測量完畢。


###其他功能
* 按鈕3可以偵測excel與14565B之連結狀態。
* 測量過程中按下按鈕7可停止測量(非暫停功能)。
* 測量的過程狀態會更新在狀態8中
* 測量完畢按按鈕下4自動關閉 14565B tool。
* 按鈕6提供單項測量功能，按下後出現提示對話框如下圖，輸入欲測量號碼即可進行單項測量，過程中依照出現之對話框訊息進行後續動作，數值同樣會自動填入表格1中。

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/excel/excel_05.jpg" width="372px" height="288px" />


## Device 端 App 使用說明
###安裝方式
將裝置經由USB連到電腦，下指令
adb install AutoPowerMeasurement_v1.6.apk 來安裝此app
P.S. Linux User 可使用 Release/setupAutoPowerMeasurement.sh 直接將 mediaFiles 推到機台並安裝 AutoPowerMeasurement_v1.6.apk

###首次使用
做完「機台初始設定」以後，即可開啟使用本 App
下圖為首頁，共有「intent 設定」、「快速測試」、「完整測試」、「中斷測試」

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_01.jpg" width="400px" height="640px" />

本 App 會發送播放 music 以及 video 的影片來執行多媒體測項，有的機台如果有不只一個可以處理這樣子的多媒體 intent ，如下面兩個圖，必須先在此步驟選取要執行的應用程式，並點選「一律採用」方便之後的自動化測試

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_02.jpg" width="400px" height="640px" />
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_03.jpg" width="400px" height="640px" />

第一次使用建議先點選「快速測試」來快速跑過所有測項
由於本 App 會使用到鎖屏的權限來讓裝置進入 suspend 的狀態，所以第一次測試前會要求使用者開啟權限，如下圖。
點選啟用以後會自動開始執行測項

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_04.jpg" width="400px" height="640px" />


###開始完整測試
點選「完整測試」，畫面會回到Home，並且跳出提示對話框，然後同時按下 PC 端與裝置端之確定鈕開始量測 Home 與 Setting 測項

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_05.jpg" width="400px" height="640px" />
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_06.jpg" width="400px" height="640px" />


再來螢幕會暗掉播放音樂，接著依序播放 720p 以及 1080p 測項的影片，如下圖所示

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_07.jpg" width="640px" height="400px" />
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_08.jpg" width="640px" height="400px" />

影片測試結束後，系統會進入idle (display off) 的狀態，此時螢幕關閉但仍持有 wakelock，經過一段時間後會解 鎖 wakelock，進入 suspend 的狀態，這兩個測項結束以後會自動出現插入耳機的提示，一樣同時按下電腦與裝置端的確定按鈕開始進行測量。
耳機的測項結束後會跳出對話框提示要連接 HDMI 線，接上線以後同時按下電腦與裝置端的確定按鈕開始進行測量

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_09.jpg" width="400px" height="640px" />
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_10.jpg" width="400px" height="640px" />

HDMI 測試結束後，接下來會量測 suspend with WIFI + BT + GPS on，由於 Android SDK 的限制，Wifi 跟 BT 可以透過程式開關，但是 GPS 必須要手動開啟如下圖

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_11.jpg" width="400px" height="640px" />
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_12.jpg" width="400px" height="640px" />

GPS 開啟後我們會嘗試在 30 秒內搜尋 GPS_FIRST_FIX 確認有收到第一個位置訊號，有收到訊號或是 30 秒內沒收到的話會跳出提示框確認進行該測項，記住 Wifi 打開以後應該會自動連上之前記憶過的 Wifi，確認沒問題後同時按下電腦與裝置端的確定按鈕開始進行測量

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_13.jpg" width="400px" height="640px" />
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_14.jpg" width="400px" height="640px" />

最後進行的是 Suspend with Camp to live network + Data OFF，這個測項是在皆 Sim 卡的情況下，將 Network Data disable 的情況下量測有 telephony 訊號下的耗電，Wifi 以及 BT 已經自動關閉，需要手動關閉 GPS

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_15.jpg" width="400px" height="640px" />

接下來必須關閉飛航模式來 enable telephony data，直接從上方 status bar 關閉飛航模式即可

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_16.jpg" width="400px" height="640px" />
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_17.jpg" width="400px" height="640px" />

測試結束後會跳出對話框提示測量已經全部結束，如果測試過程有問題最後可以分享此次的測試 Log

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_18.jpg" width="400px" height="640px" />
<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_19.jpg" width="400px" height="640px" />

###中斷測試
在測試進行中，回到 App 點選中斷測試即可結束進行中的測試
或是在任何一次的提示框點選取消也會有一樣的效果

###移除apk
由於此app有使用到裝置管理員的權限，沒有辦法直接刪除，必須到Settings > Security > Device administrators 裡頭將Auto Power Measurement取消勾選，接著才可以移除本App

<img src="https://github.com/KenjiChao/AutoPowerMeasurement/blob/master/images/app/app_20.jpg" width="400px" height="640px" />

## License
```
Copyright (C)  JB Liu, and Kenji Chao

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```












