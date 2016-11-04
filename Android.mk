LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := \
        android-support-v13 \
        libjunrar \
        libzip4j \
        #android-support-v4 \

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := FPFileExplorer

LOCAL_OVERRIDES_PACKAGES := FileExplorer

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libjunrar:libs/junrar-0.7.jar \
                                        libzip4j:libs/zip4j_1.3.1.jar
include $(BUILD_MULTI_PREBUILT)

# additionally, build tests in sub-folders in a separate .apk
include $(call all-makefiles-under,$(LOCAL_PATH))
