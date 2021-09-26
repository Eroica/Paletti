project('Paletti', 'vala', 'c')

vapi_dir = meson.current_source_dir() / 'vapi'
add_project_arguments(['--vapidir', vapi_dir, '--enable-experimental-non-null'], language: 'vala')

dependencies = [
    dependency('glib-2.0'),
    dependency('gobject-2.0'),
    dependency('gtk4'),
    dependency('lept')
]

sources = files('src/main.vala',
                'src/model.vala',
                'src/leptonica.vala',
                'src/color.vala',
                'src/window.vala',
                'src/scene.vala',
                'src/dialog.vala',
                'src/notification.vala')

sources += import('gnome').compile_resources(
    'paletti-resources',
    'src/paletti.gresource.xml',
    source_dir: 'src',
    c_name: 'paletti'
)

executable('Paletti', sources, dependencies: dependencies, install : true)
