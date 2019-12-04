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
import matplotlib.pyplot as plt
from sklearn.metrics import confusion_matrix
import pandas as pd

np.set_printoptions(threshold=np.nan)

# Training the model and prediction
def training_Prediction():

	base_path = "../data/eeg_fpz_cz" # Please specify the corresponding path before executing this file 

	files = sorted(glob(os.path.join(base_path, "*.npz")))

	ids = list(set([x.split("/")[-1][:5] for x in files])) # test if the file access is correct
	list_f1 = []
	list_acc = []
	preds = []
	gt = []
	for id in ids:
		test_ids = {id}
		train_ids = set([x.split("/")[-1][:5] for x in files]) - test_ids



	   
		train_val, test = [x for x in files if x.split("/")[-1][:5] in train_ids],\
						  [x for x in files if x.split("/")[-1][:5] in test_ids]

		train, val = train_test_split(train_val, test_size=0.1, random_state=1337)


		train_dict = {k: np.load(k) for k in train}
		test_dict = {k: np.load(k) for k in test}
		val_dict = {k: np.load(k) for k in val}

		print("validating "+str(val_dict))
		print("testing "+str(test_dict))
		
		# model = get_model_cnn_crf(lr=0.0001)
		# model=get_model()
		model = get_model_cnn()

		# print("Gen_Dict: "+str(gen(train_dict)))

		file_save_path = "check_cnnZwake_model_20_fold.h5"
		file_load_path = "check_cnnZwake_model_20_fold.h5"

		# Training 
		checkpoint = ModelCheckpoint(file_save_path, monitor='val_acc', verbose=1, save_best_only=True, mode='max')
		early = EarlyStopping(monitor="val_acc", mode="max", patience=20, verbose=1)
		redonplat = ReduceLROnPlateau(monitor="val_acc", mode="max", patience=5, verbose=2)
		callbacks_list = [checkpoint, redonplat]  # early
		model.fit_generator(gen(train_dict, aug=False), validation_data=gen(val_dict), epochs=25, verbose=2, steps_per_epoch=1000, validation_steps=300, callbacks=callbacks_list)
		model.save(file_save_path)
		model=load_model(file_load_path)

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



	f1 = f1_score(gt, preds, average="macro")

	pd.DataFrame(gt).to_csv("groundCheck.csv",header=None)
	pd.DataFrame(preds).to_csv("predictionsCheck.csv",header=None)
	
	acc = accuracy_score(gt, preds)

	print("acc_20fold %s, f1_20fold %s"%(acc, f1))

	print(classification_report(gt, preds))

	print()

	print(confusion_matrix(gt, preds))



# Saving the trained model
def train_Save():

	data_loader = NonSeqDataLoader(
	                    data_dir="path_to/data/eeg_fpz_cz", 
	                    n_folds=20, 
	                    fold_idx=0
	                )

	file_path = "Full_cnn_model_20_folds.h5"

	# Training 
	checkpoint = ModelCheckpoint(file_path, monitor='val_acc', verbose=1, save_best_only=True, mode='max')
	early = EarlyStopping(monitor="val_acc", mode="max", patience=20, verbose=1)
	redonplat = ReduceLROnPlateau(monitor="val_acc", mode="max", patience=5, verbose=2)
	callbacks_list = [checkpoint, redonplat]  # early
	x_train, y_train, x_valid, y_valid = data_loader.load_train_data()

	x_train=tf.squeeze(x_train)
	np.shape(x_train)
	model=get_model()
	model.fit(x=x_train,y=y_train, epochs=40, verbose=2, steps_per_epoch=1000, validation_steps=300, callbacks=callbacks_list)
	model.save(file_path)


# Testing from the saved model
def test_Inference(X):
	
	print ("Loading Model")
	model = get_model_cnn_crf(lr=0.0001)
	# file_path = "cnn_crf_model_20_folds.h5"
	model.load_weights(file_path)
	

	print ("Inferencing")
	Y_pred = model.predict(X)
	Y_pred = Y_pred.argmax(axis=-1).ravel().tolist()

	print(Y_pred)


# Main function
if __name__=="__main__":


	# train_Save()
	training_Prediction()
	# test_Inference()