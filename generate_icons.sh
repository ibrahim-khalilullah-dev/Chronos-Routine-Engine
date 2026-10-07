#!/bin/bash
densities=("mdpi:48" "hdpi:72" "xhdpi:96" "xxhdpi:144" "xxxhdpi:192")

for entry in "${densities[@]}"; do
  density="${entry%%:*}"
  size="${entry##*:}"
  dir="app/src/main/res/mipmap-$density"
  mkdir -p "$dir"

  # Squircle One UI Launcher Icon (smooth antialiased rounded rectangle)
  radius=$((size * 26 / 100))
  center=$((size / 2))
  r1=$((size * 34 / 100))
  r2=$((size * 28 / 100))
  stroke=$((size * 3 / 100))
  if [ $stroke -lt 2 ]; then stroke=2; fi

  convert -size ${size}x${size} xc:none \
    -fill "#0B0E14" -draw "roundrectangle 0,0,$((size-1)),$((size-1)),$radius,$radius" \
    -stroke "#312E81" -strokewidth $stroke -fill none -draw "circle $center,$center,$center,$((center - r2))" \
    -stroke "#6366F1" -strokewidth $stroke -fill none -draw "arc $((center - r2)),$((center - r2)) $((center + r2)),$((center + r2)) 270,360" \
    -stroke "#06B6D4" -strokewidth $stroke -fill none -draw "arc $((center - r2)),$((center - r2)) $((center + r2)),$((center + r2)) 0,90" \
    -stroke "#10B981" -strokewidth $stroke -fill none -draw "arc $((center - r2)),$((center - r2)) $((center + r2)),$((center + r2)) 90,160" \
    -stroke "#FFFFFF" -strokewidth $((stroke * 4 / 5)) -draw "line $center,$center $((center - size*15/100)),$((center - size*15/100))" \
    -stroke "#67E8F9" -strokewidth $((stroke * 3 / 5)) -draw "line $center,$center $((center + size*18/100)),$((center - size*16/100))" \
    -stroke none -fill "#FFFFFF" -draw "circle $center,$center,$center,$((center - size*3/100))" \
    "$dir/ic_launcher.png"

  # Round Launcher Icon
  convert -size ${size}x${size} xc:none \
    -fill "#0B0E14" -draw "circle $center,$center,$center,0" \
    -stroke "#312E81" -strokewidth $stroke -fill none -draw "circle $center,$center,$center,$((center - r2))" \
    -stroke "#6366F1" -strokewidth $stroke -fill none -draw "arc $((center - r2)),$((center - r2)) $((center + r2)),$((center + r2)) 270,360" \
    -stroke "#06B6D4" -strokewidth $stroke -fill none -draw "arc $((center - r2)),$((center - r2)) $((center + r2)),$((center + r2)) 0,90" \
    -stroke "#10B981" -strokewidth $stroke -fill none -draw "arc $((center - r2)),$((center - r2)) $((center + r2)),$((center + r2)) 90,160" \
    -stroke "#FFFFFF" -strokewidth $((stroke * 4 / 5)) -draw "line $center,$center $((center - size*15/100)),$((center - size*15/100))" \
    -stroke "#67E8F9" -strokewidth $((stroke * 3 / 5)) -draw "line $center,$center $((center + size*18/100)),$((center - size*16/100))" \
    -stroke none -fill "#FFFFFF" -draw "circle $center,$center,$center,$((center - size*3/100))" \
    "$dir/ic_launcher_round.png"
done
rm generate_icons.sh