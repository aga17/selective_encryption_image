@echo off
echo Reconstructing DECRYPTED video from frames...
echo.
echo Using FFmpeg to combine decrypted frames:
ffmpeg -framerate 30.00 -i "src\main\resources\temp\video\decrypted_frames\frame_%%06d.png" -c:v libx264 -pix_fmt yuv420p "src\main\resources\output\decrypted\video\decrypted_video.mp4"
echo.
echo Decrypted video will be saved as: src\main\resources\output\decrypted\video\decrypted_video.mp4
echo.
pause
