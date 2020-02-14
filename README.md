# Real Time Sleep Staging using Deep Learning on 1-Channel EEG

## Model Generation

```
cd ModelGeneration
```

### Environment

* Ubuntu 16.04
* CUDA toolkit 8.0 and CuDNN v5
* Python 2.7
* tensorflow-gpu (0.12.1)
* matplotlib (1.5.3)
* scikit-learn (0.19.1)
* scipy (1.0.0)
* numpy (1.11.1)
* pandas (0.18.1)
* mne (0.15.2)

### Preparing the dataset and pre-processing

#### For the Sleep-EDF dataset, you can run the following scripts to download SC subjects.

```
cd data
chmod +x download_physionet.sh
./download_physionet.sh
cd ..
```

#### Then run the following script to extract specified EEG channels and their corresponding sleep stages.

```
python prepare_physionet.py --data_dir data --output_dir data/eeg_fpz_cz --select_ch 'EEG Fpz-Cz'
python prepare_physionet.py --data_dir data --output_dir data/eeg_pz_oz --select_ch 'EEG Pz-Oz'
```

### Training the model and saving it as keras .h5 model

```python
python cnn_crf_model_20_folds.py > testChecking.txt
```

### Convert .h5 to .tflite model

```python
import tensorflow as tf

converter = tf.lite.TFLiteConverter.from_keras_model_file("saved_model_name.h5")
tflite_model = converter.convert()
open("converted_model.tflite", "wb").write(tflite_model)
```

## Real-time Sleep Staging Smartphone Application

Developed with Android Studio

### Environment

* Java
* Hardware Architectures Supported: armeabi-v7a

### Preparing the APK in Android Studio
```
Ensure the correctness of the corresponding PATHS of SDK, NDK, CMake, JDK and other files
Enable USB Debugging in the Android Smartphone Developer Options
Run and generate corresponding APK to install
```

### Running the already built APK on android device
```
Copy the existing app-debug.apk from app\build\outputs\apk\debug to your device and install
```

### App Usage Instructions
1. Ensure the required permissions are granted.
2. Turn on the Muse EEG device and connect it to the app by clicking on the dropdown spinner in the beginning of the app.
3. Refresh and connect until the Muse Device connects.
4. Click on the view toggle-button to visualize EEG from Tp10 electrode.
5. Click on the EEG Data toggle button and ensure all the electrodes are giving valid outputs.
6. Hypnograms will be automatically updated and also recorded on to the data file of the recording session.
6. Disconnect once the recording is complete.

### Additional Features
1. Charge % of Muse Device, Relative Power Spectral Band Values, Accelerometer and Gyrometer Data along with Blink and Jaw Clench artefacts are shown in the app.
2. 'Choose Sound' option with corresponding time (sec) to be entered, allows for the alarm sound and duration to be set in the REM cycle.
3. Upload feature to dropbox is also available with correpsonding instruction to be made to the app.
4. Frontal Asymmetry Index value as a running sum of the ln(alpha_power_of_the_right_frontal electrode/alpha_power_of_the_left_frontal electrode) is also available.

Automatically enumerated and time-stamped data_file.csv and corresponding full recordings of EEG raw and normalized values for analysis is to be found in the smartphone directory given below.
```
Local/Android/data/com.medialab.realtimesleep/files/data
```

Visit the MainActivity.java file of the app to play-around with the features and commented code to understand the working better.


### References:

1) Model reference - CVxTz - [EEG_Classification](https://github.com/CVxTz/EEG_classification)

2) Preprocessing reference -Akara - [DeepSleepNet](https://github.com/akaraspt/deepsleepnet)

# If you find this helpful, please cite:

[Real-time Sleep Staging using Deep Learning on 1-Channel EEG](https://ieeexplore.ieee.org/abstract/document/8771091/) 

```
@inproceedings{koushik2019real,
  title={Real-time Smartphone-based Sleep Staging using 1-Channel EEG},
  author={Koushik, Abhay and Amores, Judith and Maes, Pattie},
  booktitle={2019 IEEE 16th International Conference on Wearable and Implantable Body Sensor Networks (BSN)},
  pages={1--4},
  year={2019},
  organization={IEEE}
}
```
[Real-Time Sleep Staging using Deep Learning on a Smartphone for a Wearable EEG](https://arxiv.org/abs/1811.10111) 

```
@article{koushik2018real,
  title={Real-Time Sleep Staging using Deep Learning on a Smartphone for a Wearable EEG},
  author={Koushik, Abhay and Amores, Judith and Maes, Pattie},
  journal={arXiv preprint arXiv:1811.10111},
  year={2018}
}
```


