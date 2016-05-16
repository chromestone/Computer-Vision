import socket
import time
from robotcontroll import RobotControll
from tester import BrickGetter
from nxt.sensor.hitechnic import MotorCon
import array
import json
import os
import os.path

#read 1 byte at a time (assumes input very "primitive")
BUFFER_SIZE = 1 # Normally 1024, but we want fast response

def main():

	if (os.path.isfile('nxt-server.config')):

		try:
			with open('nxt-server.config', 'r') as configFile:    
				data = json.load(configFile)
		except ValueError:
			print 'Check config file (format).'
			return
		except IOError:
			print "Someting vewy wong!"
			return

	else:

		data = json.loads(json.dumps({'ip' : 'localhost', 'port' : 5005, 'id' : 'MUST PUT ID HERE FROM NXT','debug' : False}, sort_keys = True))
		with open('nxt-server.config', 'w') as configFile:
			json.dump(data, configFile, indent = 4, sort_keys = True)
		print 'you will find the config file here (MUST EDIT):'
		print os.getcwd()
		return
	
	#get the brick
	print 'Brick get'
	bG = BrickGetter()
	bG.ID = data['id']
	brick = bG.getBrick()
	print 'Brick got'

	#get TCP connection
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.bind((data['ip'], data['port']))

	inputLength = 1;
	while (inputLength > 0):

		s.listen(1)
		print 'Starting session...'
		print 'Waiting for connection...'
		conn, addr = s.accept()
		print 'Connection address:', addr
		print 'Started session'

		#set variables used to power motors, etc.
		#motorCont = MotorCon(brick, 0)
		motorConLeft = MotorCon(brick, 0)#************
		#motorConRight = MotorCon(brick, 1)

		receivedData = array.array('c');
		while True:

			data = conn.recv(BUFFER_SIZE)

			#if data is None, (connect assumed to be close) exit
			if not data:
				break

			#pipeline represents end 
			if '|' == data:

				command = receivedData.tostring()

				args = command.split(':')
				if len(args) > 1:

					#print args #DEBUGGING <--------- HERE

					if 'lr' == args[0]:

						if (len(args) > 2):

							lPower = int(args[1])
							rPower = int(args[2])
							motorConLeft.set_power(2, lPower)#************
							#motorConRight.set_power(2, rPower)
							motorConLeft.set_power(1, rPower)#************
							#motorConRight.set_power(1, rPower)

					elif 'a' == args[0]:

						power = int(args[1])
						motorConLeft.set_power(2, power)#************
						#motorConRight.set_power(2, power)
						motorConLeft.set_power(1, power)#************
						#motorConRight.set_power(1, power)

				#"OK" signal to client
				if conn.send('|') == 0:
					print 'socket connection broken'
					break

				if (data['debug']):

					print receivedData

				#"clear" array
				receivedData = array.array('c');

			else:

				receivedData.append(data);

			#print '[' + data + ']' #DEBUGGING <--------- HERE

		#print 'Left over data:'
		#print receivedData.tolist() #DEBUGGING <--------- HERE

		print 'Ending session...'

		print 'Disabling motors...'
		#motorCont.set_power(2, 0)

		motorConLeft.set_power(2, 0)#************
		#motorConRight.set_power(2, 0)
		motorConLeft.set_power(1, 0)#************
		#motorConRight.set_power(1, 0)

		print 'Ended session'
		print 'Press enter to terminate program; enter any character(s) to continue'
		inputLength = len(raw_input())

if __name__ == "__main__":

	main()