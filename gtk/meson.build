project('Paletti', 'vala', 'c',
         version: 'v2024.04a',
         meson_version: '>= 0.62.0',
         default_options: [ 'warning_level=2', 'werror=false', ])

i18n = import('i18n')
gnome = import('gnome')

vapi_dir = meson.current_source_dir() / 'vapi'
add_project_arguments(['--vapidir', vapi_dir], language: 'vala')

subdir('data')
subdir('src')
subdir('po')
