Inputs (i.e. controllers) -> Java <-- TCP --> Python <-- Bluetooth --> NXT (Brick)

Python runs the TCP server:
    (Note: Only Python 2.7 currently supported)
    Dependencies:
        NXT-Python: https://github.com/Eelviny/nxt-python
        (Note: other libraries needed are listed on the page)
Java runs the TCP client:
    Dependencies:
        libGDX: http://libgdx.badlogicgames.com
        (Note: A COMPILED JAR FOR CONTROLLER INPUT [i.e. XBox Controller] IS AVAILABLE HERE: )