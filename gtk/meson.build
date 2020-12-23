project('Paletti', ['c', 'vala'],
        version: '2.0',
        meson_version: '>= 0.29.0',
        default_options: ['warning_level=2'])

vapi_dir = meson.current_source_dir() + '/vapi'

dependencies = [
    dependency('glib-2.0', version: '>=2.48'),
    dependency('gobject-2.0'),
    dependency('gtk+-3.0', version: '>=3.18'),
    dependency('lept')
]

sources = files('src/main.vala',
                'src/window.vala',
                'src/scene.vala',
                'src/dialog.vala',
                'src/notification.vala',
                'src/pix.vala',
                'src/color.vala')

sources += import('gnome').compile_resources(
    'paletti-resources',
    'src/paletti.gresource.xml',
    source_dir: 'src',
    c_name: 'paletti'
)

executable('Paletti', sources, dependencies: dependencies, install : true,
           vala_args: ['--vapidir=../vapi', '--gresources=../src/paletti.gresource.xml'])

