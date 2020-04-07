# Import modules
from models import get_model_cnn_crf,get_model,get_model_cnn 

import numpy as np
from utils import gen, chunker, WINDOW_SIZE, rescale_array, rescale_wake
from keras.callbacks import ModelCheckpoint, EarlyStopping, ReduceLROnPlateau
from keras.models import load_model
from sklearn.metrics import f1_score, accuracy_score, classification_report
import tensorflow as tf
from glob2 import glob
import os
from sklearn.model_selection import train_test_split
from tqdm import tqdm
import matplotlib     # Added to patch mpl import bug for Python 2 
matplotlib.use('agg') # Added to patch mpl import bug for Python 2
import matplotlib.pyplot as plt 
from sklearn.metrics import confusion_matrix
import pandas as pd
np.set_printoptions(precision=2)

#This Python script trains a model to predict sleep stages.

#It contains 4 functions: 
#   Training the model: train_model()
#   Calculate Accuracy of the model: eval_model()
#   Cross validate the model: cross_validation_training()
    # Repeats 20 times train & test pipeline for 20 possible partitions between train & test
    # ---> This is only needed if we want to validate our model but not necessary for deployment.

#   Training the model with all the data: full_training() 

def train_model(train_files, model_save_path):
    
    """ Main function to train model.

    Inputs:
    - train_files: files we will use to train our model. 
    - model_save_path: path to save the model. 
    """

    #Split the data between training and validation. The variable "test_size" is 0.1 (10%) the percentage for validation. 
    # ---> train_test_split is a function that randomly splits the data. For now we won't cherry pick our random state.

    train,val=train_test_split(train_files, test_size=0.1)#, random_state=1337) 

    # Load all our train data
    train_dict = {k: np.load(k) for k in train}

    # Load all our validation data	
    val_dict = {k: np.load(k) for k in val}
    print("Validating: "+str(val_dict))

    #The model architecture has 3 repeated sets of two 1-D convolutional (Conv1D) layers, 1-D max-pooling and spatial dropout layers.
    # This is followed by two Conv1D, 1-D global max-pooling, dropout and dense layers. We finally have a dropout layer as the output of "Base-CNN". 
    # This is fed to the Time-Distributed Base-CNN model, and then a 1-D convolutional layer, spatial dropout, another 1-D convolutional layer, dropout, 1D conv and finally the multiclass sleep labels.

    model = get_model_cnn()

    # Training 
    #This is useful to avoid overfitting. Saves what is the best so far for validation accuracy (for every epoch).
    checkpoint = ModelCheckpoint(model_save_path, monitor='val_acc', verbose=1, save_best_only=True, mode='max') 
    early = EarlyStopping(monitor="val_acc", mode="max", patience=20, verbose=1)

    #Learning rate is reduced each time the validation accuracy plateaus using ReduceLROnPlateau Keras Callbacks.
    redonplat = ReduceLROnPlateau(monitor="val_acc", mode="max", patience=5, verbose=2)
    callbacks_list = [checkpoint, redonplat]  

    model.fit_generator(gen(train_dict, aug=False), validation_data=gen(val_dict), epochs=25, verbose=2, steps_per_epoch=1000, validation_steps=300, callbacks=callbacks_list)

    #And finally we save our model!
    model.save(model_save_path)


def eval_model(model , test_files):

    """ Main function to evaluate (estimate accuracy) of the model.

    Inputs:
    - model: the model to train
    - test_files: files we will use to test our model (in this case 1 person). 
    """

    test_dict = {k: np.load(k) for k in test_files}
    print("Testing: "+str(test_dict))

    #Validation
    for record in tqdm(test_dict):

    	all_rows = test_dict[record]['x']
    	record_y_gt = []
    	record_y_pred = []
	
    	# for batch_hyp in chunker(range(all_rows.shape[0])): # for batchwise chunking

    	X = all_rows #[min(batch_hyp):max(batch_hyp)+1, ...]
    	Y = test_dict[record]['y'] #[min(batch_hyp):max(batch_hyp)+1]

    	wakeStd=rescale_wake(X,Y)
    	X = np.expand_dims(X, 0)
    	X=(X-np.mean(X))/wakeStd
	
        # X = rescale(X, Y) #default

    	Y_pred = model.predict(X)
    	Y_pred = Y_pred.argmax(axis=-1).ravel().tolist()


    	gtNow=Y.ravel().tolist()
    	gt += gtNow
    	preds += Y_pred

    	record_y_gt += Y.ravel().tolist()
    	record_y_pred += Y_pred
	

    	acc_Test = accuracy_score(gtNow, Y_pred)
    	f1_Test = f1_score(gtNow, Y_pred, average="macro")
    	print("acc %s, f1 %s"%(acc_Test, f1_Test))

    return gt, preds
    
# Training the model and prediction
def cross_validation_training():
    
    """ Function to cross-validate the model.
    """

    base_path = "../data/eeg_fpz_cz"     # We used the expanded Sleep-EDF database from Physionet bank. Single-channel EEG (Fpz-Cz at 100Hz) of 20 subjects.
    model_save_path = "Sleep_CNN_33.h5" # Model generated with 19 subjects (2 nights par subject) as our training-set (33 nights in total because it's 90%).

    files = sorted(glob(os.path.join(base_path, "*.npz")))

    subject_ids = list(set([x.split("/")[-1][:5] for x in files])) #Extract user ID from file name. (-1 is the last column, and :5 is the first 5 chars). The result is: 20 subject ids: SC400, SC401, SC402, SC403, SC404, SC405, SC406, SC407, SC408, SC409, SC410, SC411, SC412, SC413, SC414, SC415, SC416, SC417, SC418, SC419. list->set-> gets rid of duplicates.

    list_f1 = []
    list_acc = []
    allpreds = []
    allgt = []

    #Loop over subject which will be used as test.
    for i in subject_ids:

        test_id = set([i]) #Test id
        all_subjects = set(subject_ids)

        train_ids =  all_subjects - test_id # train_ids substracts the ids for all subjects minus 1 user (set([i]))

        #From the ids that we have found, let's get the actual files and collect the data for training and test. 
        train_files, test_files = [x for x in files if x.split("/")[-1][:5] in train_ids],\
        				  [x for x in files if x.split("/")[-1][:5] in test_id]


        #Once we know the ids we will use for our train data: 
        #LET'S TRAIN THE MODEL!
        train_model(train_files, model_save_path) #It trains the model and saves it in the same path.

        #Once we have our model:
        #LET'S EVALUATE THE MODEL (for this specific subject)!
        gt, preds = eval_model(load_model(model_save_path), test_files)

        allpreds += preds
        allgt    += gt
        
    
    f1 = f1_score(allgt, allpreds, average="macro")
    pd.DataFrame(allgt).to_csv("groundCheck.csv",header=None)
    pd.DataFrame(allpreds).to_csv("predictionsCheck.csv",header=None)

    acc = accuracy_score(allgt, allpreds)

    print("acc_20fold %s, f1_20fold %s"%(acc, f1))

    print(classification_report(allgt, allpreds))

    print()

    print(confusion_matrix(allgt, allpreds))


# Training the model with all subjects. Simplified version for deployment & with more data.
# Careful that this is only for deployment, if we want to know the accuracy of the model we need to do all the validation/cross-validation

def full_training():
    
    base_path = "../data/eeg_fpz_cz"     # We used the expanded Sleep-EDF database from Physionet bank. Single-channel EEG (Fpz-Cz at 100Hz) of 20 subjects.
    print("Loading Data from path: %s"%(base_path))
    
    model_save_path = "Sleep_CNN_40.h5" # Model generated with 20 subjects (2 nights par subject) as our training-set (40 nights in total because it's 100%).

    # We input all the data
    files = sorted(glob(os.path.join(base_path, "*.npz")))

    #Once we know the ids we will use for our train data: 
    #LET'S TRAIN THE MODEL!
    print("Training the model...")
    train_model(files, model_save_path) #It trains the model and saves it in the same path.
    print("Model Trained! You can find it here: %s" %(model_save_path))
    return (model_save_path)

# This function is useful for deployment. It loads the saved model and returns the prediction.
#def make_prediction(model_path,X):
    
#	print ("Loading Model...")
#    model = load_model(model_path)

#	print ("Prediction...")
#	Y_pred = model.predict(X)
#	Y_pred = Y_pred.argmax(axis=-1).ravel().tolist()

#	print(Y_pred)


# Main function
if __name__ == "__main__":

    #cross_validation_training() #It generates a model with 19 subjects and 1 as test (33 nights). It saves it as: "Sleep_CNN_33.h5"
    
    #-------This is for deployment and to simply use the model but not bother about the validation:-------
    
    model_path = full_training()  #It generates a model trained with 20 subjects and 1 as test (40 nights): "Sleep_CNN_40.h5"
    #make_prediction(model_path)
    