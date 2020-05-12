project('Paletti', ['c', 'vala'],
        version: '2.0',
        meson_version: '>= 0.50.0',
        default_options: ['warning_level=2'])

vapi_dir = meson.current_source_dir() / 'vapi'

add_project_arguments(['--vapidir', vapi_dir], language: 'vala')

dependencies = [
    dependency('glib-2.0', version: '>=2.38'),
    dependency('gobject-2.0'),
    dependency('gtk+-3.0'),
    dependency('lept')
]

sources = files('src/main.vala',
                'src/window.vala',
                'src/dialog.vala',
                'src/notification.vala',
                'src/image.vala',
                'src/color.vala')

sources += import('gnome').compile_resources(
    'paletti-resources',
    'src/paletti.gresource.xml',
    source_dir: 'src',
    c_name: 'paletti'
)

executable('Paletti', sources, dependencies: dependencies, install : true)
