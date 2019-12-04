import h5py
import numpy as np
import pandas as pd
import random

WINDOW_SIZE = 100


def rescale_wake(all_X,all_Y):
    all_Y = np.expand_dims(all_Y, -1)
    all_Y = np.expand_dims(all_Y, 0)
    all_X = np.expand_dims(all_X, 0)
      
    flattenedY=[val for sublist in all_Y[0] for val in sublist]
    
    # Wake State Normalization
    meanStd=[]
    for j in range(np.shape(flattenedY)[0]):
        if(flattenedY[j]==0):
            meanStd.append(np.std(all_X[0][j]))
    
    if(len(meanStd)==0):
        wakeStd=20
        print("no wake")

    wakeStd=np.mean(meanStd)
    return wakeStd

def rescale_array(X):
    
    # # Z Normalization
    # print(np.shape(X[0][0]))

    flattened=[val for sublist in X[0][0] for val in sublist]
    # X=(X-pd.Series(flattened).mean())/pd.Series(flattened).std()

    # #default Norm
    # X=(X-np.mean(X))/np.std(X)
    # print(X)

    # # default
    # X = X / 20
    # X = np.clip(X, -5, 5)
    return X


def aug_X(X):
    scale = 1 + np.random.uniform(-0.1, 0.1)
    offset = np.random.uniform(-0.1, 0.1)
    noise = np.random.normal(scale=0.05, size=X.shape)
    X = scale * X + offset + noise
    return X

def gen(dict_files, aug=False):
    while True:
        record_name = random.choice(list(dict_files.keys()))
        batch_data = dict_files[record_name]
        all_rows = batch_data['x']
        all_Y=batch_data['y']

        wakeStd=rescale_wake(all_rows,all_Y)

        for i in range(10):
            start_index = random.choice(range(all_rows.shape[0]-WINDOW_SIZE))

            X = all_rows[start_index:start_index+WINDOW_SIZE, ...] # default
            Y = batch_data['y'][start_index:start_index+WINDOW_SIZE] # default

            X = np.expand_dims(X, 0)
            Y = np.expand_dims(Y, -1)
            Y = np.expand_dims(Y, 0)

            if aug:
                X = aug_X(X)

            # print("Printing X0: "+X[0])
            # X = rescale_array(X) # rescale_array default
            X=(X-np.mean(X))/wakeStd
        
            yield X, Y


def chunker(seq, size=WINDOW_SIZE):
    return (seq[pos:pos + size] for pos in range(0, len(seq), size))