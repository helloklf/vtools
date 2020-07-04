# pm  resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN | grep '/'
pm  resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN | grep '/' | cut -f1 -d '/'
