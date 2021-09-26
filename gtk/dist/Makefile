APP_BIN_DIR=AppDir/usr/bin
APP_DESKTOP_DIR=AppDir/usr/share/applications
APP_ICON_DIR=AppDir/usr/share/icons/hicolor/256x256/apps
APP_SYMBOLIC_DIR=AppDir/usr/share/icons/hicolor/symbolic/apps

.PHONY: appimage flatpak

appimage: Paletti-x86_64.AppImage

Paletti-x86_64.AppImage: linuxdeploy-x86_64.AppImage ${APP_BIN_DIR}/Paletti ${APP_DESKTOP_DIR}/app.paletti.gtk.desktop ${APP_ICON_DIR}/app.paletti.gtk.png ${APP_SYMBOLIC_DIR}/app.paletti.gtk-symbolic.svg
	./linuxdeploy-x86_64.AppImage --appdir AppDir --output appimage

../build/Paletti:
	cd .. && meson build && cd build && meson compile

${APP_BIN_DIR}/Paletti: ../build/Paletti
	mkdir -p ${APP_BIN_DIR}
	cp $< $@

${APP_DESKTOP_DIR}/app.paletti.gtk.desktop: app.paletti.gtk.desktop
	mkdir -p ${APP_DESKTOP_DIR}
	cp $< $@

${APP_ICON_DIR}/app.paletti.gtk.png: app.paletti.gtk.png
	mkdir -p ${APP_ICON_DIR}
	cp $< $@

${APP_SYMBOLIC_DIR}/app.paletti.gtk-symbolic.svg: app.paletti.gtk-symbolic.svg
	mkdir -p ${APP_SYMBOLIC_DIR}
	cp $< $@

linuxdeploy-x86_64.AppImage:
	wget https://github.com/linuxdeploy/linuxdeploy/releases/download/continuous/$@
	chmod +x $@

flatpak:
	flatpak-builder --user --install build app.paletti.gtk.yml
