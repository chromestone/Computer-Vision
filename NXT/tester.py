# Program to make NXT brick beep using NXT Python with Bluetooth socket
#
# Simon D. Levy  CSCI 250   Washington and Lee University    April 2011

# Change this ID to match the one on your brick.  You can find the ID by doing Settings / NXT Version.  
# You will have to put a colon between each pair of digits.
ID = '00:16:53:1C:33:04'

# This is all we need to import for the beep, but you'll need more for motors, sensors, etc.
from nxt.bluesock import BlueSock
import time

class BrickGetter:

	def __init__(self):
	    self.sock = None

	def getBrick(self):
		# Create socket to NXT brick
		self.sock = BlueSock(ID)

		# On success, socket is non-empty
		if self.sock:

		   print('Connecting...');
		   # Connect to brick
		   brick = self.sock.connect()
		   print('Connected!');
		   return brick

		   # Play tone A above middle C for 1000 msec
		   # brick.play_tone_and_wait(440, 500)

		# Failure
		else:
		   print 'No NXT bricks found'

    #not neccessary since when brick is garabage collected
    #brick automatically closes
	def close(self):

	    self.sock.close()
# end class
def main():

	brickGetter = BrickGetter()
	brick = brickGetter.getBrick();
	brick.play_tone_and_wait(440, 500);

if __name__ == "__main__":
	main()