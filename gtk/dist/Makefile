APP_DIR=AppDir
APP_BIN_DIR=${APP_DIR}/usr/bin
APP_DESKTOP_DIR=${APP_DIR}/usr/share/applications

.PHONY: appimage flatpak

appimage: Paletti-x86_64.AppImage

Paletti-x86_64.AppImage: linuxdeploy-x86_64.AppImage ${APP_BIN_DIR}/Paletti ${APP_DESKTOP_DIR}/app.paletti.gtk.desktop
	./linuxdeploy-x86_64.AppImage --appdir AppDir --output appimage

../build/Paletti:
	cd .. && meson build && cd build && meson compile

${APP_BIN_DIR}/Paletti: ../build/Paletti
	mkdir -p ${APP_BIN_DIR}
	mkdir -p ${APP_DIR}/usr/share/icons
	cp -R hicolor ${APP_DIR}/usr/share/icons
	cp $< $@

${APP_DESKTOP_DIR}/app.paletti.gtk.desktop: app.paletti.gtk.desktop
	mkdir -p ${APP_DESKTOP_DIR}
	cp $< $@

linuxdeploy-x86_64.AppImage:
	wget https://github.com/linuxdeploy/linuxdeploy/releases/download/continuous/$@
	chmod +x $@

flatpak:
	flatpak-builder --user --install build app.paletti.gtk.yml
