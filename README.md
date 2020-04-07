# Real-time Smartphone-based Sleep Staging using 1-Channel EEG

## Paper
We present the first, real-time sleep staging system that uses deep learning without the need for servers in a smartphone application for a wearable EEG. We employ real-time adaptation of a single channel Electroencephalography (EEG) to infer from a Time-Distributed Convolutional Neural Network (CNN). Polysomnography (PSG) —the gold standard for sleep staging—requires a human scorer and is both complex and resource-intensive. Our work demonstrates an end-to-end, smartphone-based pipeline that can infer sleep stages in just single 30-second epochs, with an overall accuracy of 83.5% on 20-fold cross validation for 5-stage classification of sleep stages using the open Sleep-EDF dataset. For comparison, inter-rater reliability among sleep-scoring experts is about 80% (Cohen’s κ = 0.68 to 0.76). We further propose an on-device metric independent of the deep learning model which increases the average accuracy of classifying deep-sleep (N3) to more than 97.2% on 4 test nights using power spectral analysis.

[Click here to find more information about the project and to download the papers.](https://www.media.mit.edu/projects/sleep-staging-EEG/overview/)

## Citation

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

## Model Generation
### Requirements
* macOS Catalina (10.15.4)
* Python 3.7.7
* tensorflow==2.1.1
* keras==2.3.1

### Libraries
Go ahead and install all the necessary libraries to run this project in your computer. We recommend using a virtual environment & pip:

```
pyenv versions #Shows your current Python installs. Go ahead and install Python 3.7.7 if you don't have it.

#Create a virtual environment with Python 3.7.7
mkvirtualenv -p ~/.pyenv/versions/3.7.7/bin/python nameofyourproject

#Install Tensorflow
pip install tensorflow==2.1.0

#Install Keras
pip install keras==2.3.1

#Other libraries that are helpful
pip install matplotlib==3.2.1
pip install pandas==1.0.3
pip install sklearn==0.0
pip install glob2==0.7
pip install tqdm==4.45.0
pip install scikit-learn==0.22.2.post1
pip install scipy==1.4.1
pip install numpy==1.18.2

```

### Preparing the dataset and pre-processing
Navigate to your project folder and then "data". 
```
cd ModelGeneration
cd data
```

You can run the following scripts to download the physionet data.
We evaluated our model using the [Physionet Sleep-EDF datasets](https://physionet.org/content/sleep-edfx/1.0.0/) published in 2013. We have used the [DeepSleepNet source code](https://github.com/akaraspt/deepsleepnet) to prepare the dataset with the following lines of code:

```
chmod +x download_physionet.sh
./download_physionet.sh
cd ..

#Then run the following script to extract specified EEG channels and their corresponding sleep stages.

python prepare_physionet.py --data_dir data --output_dir data/eeg_fpz_cz --select_ch 'EEG Fpz-Cz'
python prepare_physionet.py --data_dir data --output_dir data/eeg_pz_oz --select_ch 'EEG Pz-Oz'
```

### Training the model and saving it as keras .h5 model

```python
python cnn_crf_model_20_folds.py > testChecking.txt
#or simply:
python cnn_crf_model_20_folds.py 
```
#### References:

1) Model reference - CVxTz - [EEG_Classification](https://github.com/CVxTz/EEG_classification)
2) Preprocessing reference -Akara - [DeepSleepNet](https://github.com/akaraspt/deepsleepnet)

-------------------------
## Preparing the model to run in Android
The following instructions where tested with a model that ran in an environment using Python 2 (see commits c358c0a). We have not updated the code since then:

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

### Convert .h5 to .tflite model

```python
import tensorflow as tf

converter = tf.lite.TFLiteConverter.from_keras_model_file("saved_model_name.h5")
tflite_model = converter.convert()
open("converted_model.tflite", "wb").write(tflite_model)
```
-------------------------
## Android App

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









