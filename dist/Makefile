APP_ICON_DIR=AppDir/usr/share/icons/hicolor/128x128/apps
APP_SYMBOLIC_DIR=AppDir/usr/share/icons/hicolor/symbolic/apps

appimage: Paletti-x86_64.AppImage

Paletti-x86_64.AppImage: linuxdeploy-x86_64.AppImage ../build/Paletti com.moebots.Paletti.desktop ${APP_ICON_DIR}/paletti.png ${APP_SYMBOLIC_DIR}/paletti-symbolic.svg
	./linuxdeploy-x86_64.AppImage --appdir AppDir --executable=../build/Paletti --desktop-file=com.moebots.Paletti.desktop --output appimage

${APP_ICON_DIR}/paletti.png: paletti.png
	mkdir -p ${APP_ICON_DIR}
	cp $< $@

${APP_SYMBOLIC_DIR}/paletti-symbolic.svg: paletti-symbolic.svg
	mkdir -p ${APP_SYMBOLIC_DIR}
	cp $< $@

linuxdeploy-x86_64.AppImage:
	wget https://github.com/linuxdeploy/linuxdeploy/releases/download/continuous/$@
	chmod +x $@

