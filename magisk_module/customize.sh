#!/system/bin/sh
# Magisk module installation script
ui_print "*********************************"
ui_print " 8D Pirate Audio DSP Injector   "
ui_print "*********************************"
ui_print " - Installing Zygisk module..."
set_perm $MODPATH/zygisk_module.so 0 0 0644