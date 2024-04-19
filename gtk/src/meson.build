paletti_sources = [
  'main.vala',
  'application.vala',
  'views/scene.vala',
  'views/notification.vala',
  'views/color_palette.vala',
  'window.vala',
  'color.vala',
  'leptonica.vala',
  'model.vala',
]

paletti_deps = [
  dependency('gtk4'),
  dependency('libadwaita-1', version: '>= 1.4'),
  dependency('lept'),
]

paletti_sources += gnome.compile_resources('paletti-resources',
  'paletti.gresource.xml',
  c_name: 'paletti'
)

executable('Paletti', paletti_sources,
  dependencies: paletti_deps,
       install: true,
)
