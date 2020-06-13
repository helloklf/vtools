if [[ $w == "" ]]; then
    settings put global window_animation_scale 1
fi
if [[ $transition == "" ]]; then
    settings put global transition_animation_scale 1
fi
if [[ $animator == "" ]]; then
    settings put global animator_duration_scale 1
fi

settings put global window_animation_scale $w
settings put global transition_animation_scale $transition
settings put global animator_duration_scale $animator
echo 'OK!'
