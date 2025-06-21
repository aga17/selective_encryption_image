@echo off
echo Reconstructing video from encrypted frames...
echo.
echo Using FFmpeg to combine frames:
ffmpeg -framerate 30.00 -i "src\main\resources\temp\video\encrypted_frames\frame_%%06d.png" -c:v libx264 -pix_fmt yuv420p "src\main\resources\output\encrypted\video\encrypted_video.mp4"
echo.
echo If FFmpeg is not installed, you can:
echo 1. Download FFmpeg from https://ffmpeg.org/
echo 2. Or use any video editing software to combine the PNG frames
echo.
pause
