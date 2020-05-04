APP_BIN_DIR=AppDir/usr/bin
APP_DESKTOP_DIR=AppDir/usr/share/applications
APP_ICON_DIR=AppDir/usr/share/icons/hicolor/128x128/apps
APP_SYMBOLIC_DIR=AppDir/usr/share/icons/hicolor/symbolic/apps

appimage: Paletti-x86_64.AppImage

Paletti-x86_64.AppImage: linuxdeploy-x86_64.AppImage ${APP_BIN_DIR}/Paletti ${APP_DESKTOP_DIR}/com.moebots.Paletti.desktop ${APP_ICON_DIR}/com.moebots.Paletti.png ${APP_SYMBOLIC_DIR}/com.moebots.Paletti-symbolic.svg
	./linuxdeploy-x86_64.AppImage --appdir AppDir --output appimage

${APP_BIN_DIR}/Paletti: ../build/Paletti
	mkdir -p ${APP_BIN_DIR}
	cp $< $@

${APP_DESKTOP_DIR}/com.moebots.Paletti.desktop: com.moebots.Paletti.desktop
	mkdir -p ${APP_DESKTOP_DIR}
	cp $< $@

${APP_ICON_DIR}/com.moebots.Paletti.png: com.moebots.Paletti.png
	mkdir -p ${APP_ICON_DIR}
	cp $< $@

${APP_SYMBOLIC_DIR}/com.moebots.Paletti-symbolic.svg: com.moebots.Paletti-symbolic.svg
	mkdir -p ${APP_SYMBOLIC_DIR}
	cp $< $@

linuxdeploy-x86_64.AppImage:
	wget https://github.com/linuxdeploy/linuxdeploy/releases/download/continuous/$@
	chmod +x $@

