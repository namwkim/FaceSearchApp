LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include /Users/namwkim85/Development/OpenCV-2.4.9-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := FaceSearcher


LOCAL_C_INCLUDES += $(LOCAL_PATH)/include


CLASSES_FILES   := $(wildcard $(LOCAL_PATH)/*.cpp)
CLASSES_FILES   := $(CLASSES_FILES:$(LOCAL_PATH)/%=%)

LOCAL_SRC_FILES := $(CLASSES_FILES)

LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
