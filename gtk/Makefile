# This Makefile is only for creating a flatpak image

.PHONY: Paletti

Paletti: dist/Paletti.flatpak

repo:
	mkdir -p dist/repo
	flatpak-builder --repo=dist/repo builddir app.paletti.gtk.json --force-clean

dist/Paletti.flatpak: repo
	flatpak build-bundle dist/repo dist/Paletti.flatpak app.paletti.gtk
