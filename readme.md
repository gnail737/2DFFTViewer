This is a 2D FFT implementation on Android, it transforms any PNG image (stored in $External_Dir/DCIM/browser-photos/ path into Frequency Domain Magnitude Image, note the magnitude scaling is rather arbitrary and clamping is done to preserve [0-255] color range.

The whole implementation is CPU based using Java language, I wrote both Radix-2 Cooley Tukey FFT and Matrix Ops function myself so performance is not as fast as other FFT libraries on Internet.
