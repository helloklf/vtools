settings put global airplane_mode_on 1
am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true
