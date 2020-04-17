# 2d_rendering_benchmark
Quick comparison of various canvas style libraries (e.g. cairo, java 2D, html canvas)

The test is very simple and only meant to gauge frame per seconds for simple use, in this case drawing a thousand-and-one simple eyes per frame.

The result on my desktop machine are:
* [13 fps] cairo/x11 (fastest)
* [11 fps] firefox canvas Linux
* [08 fps] firefox canvas Windows
* [07 fps] cairo/sdl
* [03 fps] java 2D

My machine was built in 2014 and includes:
* AMD FX-8320 Vishera 8-Core 3.5GHz
* GIGABYTE GV-N760WF2OC-2GD G-SYNC Support GeForce
* Crucial Ballistix Sport 8GB 240-Pin DDR3 SDRAM DDR
* GIGABYTE GA-990FXA-UD3 AM3+ AMD 990FX + SB950 SATA
* Arch Linux up to date 2020-04 with proprietary nvidia driver
* Windows 10
