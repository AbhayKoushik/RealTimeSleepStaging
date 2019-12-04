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

### Reference Credits: 

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


