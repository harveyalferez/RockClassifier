import tensorflow as tf
from tensorflow.keras.preprocessing.image import ImageDataGenerator 
from tensorflow.keras.models import Sequential 
from tensorflow.keras.layers import Conv2D, MaxPooling2D 
from tensorflow.keras.layers import Dropout, Flatten, Dense 
from tensorflow.keras import backend as K 
from tensorflow.keras import optimizers
import matplotlib.pyplot as plt
from livelossplot.tf_keras import PlotLossesCallback
import os
import numpy as np
from sklearn.metrics import classification_report

trainData = './gabbro_granite_diorite_granodirite/train'
validationData = './gabbro_granite_diorite_granodirite/validate'

epochs = 100
steps = 1000 
batch_size = 32

if K.image_data_format() == 'channels_first': 
    input_shape = (3, 100, 100) 
else: 
    input_shape = (100, 100, 3) 

model = Sequential()
model.add(Conv2D(32, (3,3), padding="same", input_shape = input_shape, activation='relu'))
model.add(MaxPooling2D(pool_size=(2,2)))
model.add(Conv2D(64, (2,2), padding="same", activation='relu'))
model.add(MaxPooling2D(pool_size=(2,2)))
model.add(Flatten())
model.add(Dense(64, activation='relu'))
model.add(Dropout(0.5))
model.add(Dense(4, activation='softmax'))
  
model.compile(loss='categorical_crossentropy', optimizer=optimizers.Adam(lr=0.001, beta_1=0.9, beta_2=0.999, epsilon=1e-08, decay=0.0), metrics=["accuracy"])
  
train_datagen = ImageDataGenerator(rescale = 1. / 255, shear_range = 0.3, zoom_range = 0.3, horizontal_flip = True) 
  
test_datagen = ImageDataGenerator(rescale = 1. / 255) 

train_generator = train_datagen.flow_from_directory(
    trainData, 
    target_size =(100, 100), 
    batch_size = batch_size, 
    class_mode ='categorical',
    shuffle=True) 
  
validation_generator = test_datagen.flow_from_directory(
    validationData, 
    target_size =(100, 100), 
    batch_size = batch_size, 
    shuffle=False) 

with tf.device('/gpu:0'):
    trainingmodel = model.fit_generator(train_generator, steps_per_epoch = steps // batch_size, epochs = epochs, validation_data = validation_generator, validation_steps = 200 // batch_size, callbacks=[PlotLossesCallback()], verbose=1) 

print(model.summary())

model.save_weights('gabbro_granite_diorite_granodiorite_weights.h5')
model.save('gabbro_granite_diorite_granodiorite.h5')

train_gabbro_dir = os.path.join(trainData, 'Gabbro')
train_granite_dir = os.path.join(trainData, 'Granite')
train_diorite_dir = os.path.join(trainData, 'Diorite')
train_granodiorite_dir = os.path.join(trainData, 'Granodiorite')

validation_gabbro_dir = os.path.join(validationData, 'Gabbro')
validation_granite_dir = os.path.join(validationData, 'Granite')
validation_diorite_dir = os.path.join(validationData, 'Diorite')
validation_granodiorite_dir = os.path.join(validationData, 'Granodiorite')

num_gabro_tr = len(os.listdir(train_gabbro_dir))
num_granite_tr = len(os.listdir(train_granite_dir))
num_diorite_tr = len(os.listdir(train_diorite_dir))
num_granodiorite_tr = len(os.listdir(train_granodiorite_dir))

num_gabbro_val = len(os.listdir(validation_gabbro_dir))
num_granite_val = len(os.listdir(validation_granite_dir))
num_diorite_val = len(os.listdir(validation_diorite_dir))
num_granodiorite_val = len(os.listdir(validation_granodiorite_dir))

total_train = num_gabro_tr + num_granite_tr + num_diorite_tr + num_granodiorite_tr
total_val = num_gabbro_val + num_granite_val + num_diorite_val + num_granodiorite_val

test_steps_per_epoch = np.math.ceil(validation_generator.samples / validation_generator.batch_size)

with tf.device('/gpu:0'):
    predictions = model.predict_generator(validation_generator, steps=test_steps_per_epoch)
predicted_classes = np.argmax(predictions, axis=1)

true_classes = validation_generator.classes
class_labels = list(validation_generator.class_indices.keys()) 

report = classification_report(true_classes, predicted_classes, target_names=class_labels)
print(report)  