
all: native

native: jni/Android.mk
	if [ a == a$(ANDROID_NDK_HOME) ]; then \
		echo ANDROID_NDK_HOME is not set;\
		exit 1;\
	fi;\
	pushd jni/xTun || exit 1;\
	dist-build/android-armv8-a.sh || exit 1;\
	popd;\
	pushd jni;\
	install -d arm64-v8a;\
	install xTun/xTun-android-armv8-a/3rd/libsodium/src/libsodium/.libs/libsodium.a arm64-v8a;\
	install xTun/xTun-android-armv8-a/3rd/libuv/.libs/libuv.a arm64-v8a;\
	install xTun/xTun-android-armv8-a/libxTun.a arm64-v8a;\
	$(ANDROID_NDK_HOME)/ndk-build NDK_LOG=1 V=1 || exit 1;\
	popd;\
	install -d app/src/main/jniLibs/arm64-v8a;\
	install libs/arm64-v8a/libxTun.so app/src/main/jniLibs/arm64-v8a;

clean:
	rm -rf libs
	rm -rf app/src/main/jniLibs
	rm -rf jni/xTun/xTun-android-armv8-a/{libxTun.a,src}
	$(ANDROID_NDK_HOME)/ndk-build clean

distclean: clean
	rm -rf obj
	rm -rf jni/arm64-v8a
	rm -rf jni/xTun/xTun-android-armv8-a

.PHONY: clean