application_id = 'app.paletti.gtk'


icon_sizes = ['16', '22', '32', '64', '128', '256', '512']

foreach i : icon_sizes
    install_data (
        join_paths ('hicolor/', i + 'x' + i, 'apps', application_id + '.png'),
        install_dir: join_paths (get_option ('datadir'), 'icons', 'hicolor', i + 'x' + i, 'apps')
    )
endforeach

symbolic_dir = 'hicolor' / 'symbolic' / 'apps'
install_data(
  symbolic_dir / ('@0@-symbolic.svg').format(application_id),
  install_dir: get_option('datadir') / 'icons' / symbolic_dir
)
