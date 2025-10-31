import json
import websocket
try:
	import thread
except ImportError:
	import _thread as thread
import time

# ^^^^^^^^^^^^^^^^^^^^^^^^  												^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
# ^^^^^^^^^^^^^^^^^^^^^^^^  												^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
# ^^^^^^^^^^^^^^^^^^^^^^^^ 行情对接地址：http://39.107.99.235:1008/market 	^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
# ^^^^^^^^^^^^^^^^^^^^^^^^  												^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
# ^^^^^^^^^^^^^^^^^^^^^^^^  												^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


# 重连标志和错误状态
should_reconnect = False
error_occurred = False

def on_data(ws, message, msg_type, flag):
	# 解析接收到的数据
	json_data = json.loads(message)

	#只处理body内的数据
	if 'body' not in json_data or not json_data['body']:
		return

	body = json_data['body']

	StockCode = body['StockCode']
	Price = body['Price']
	Open = body['Open']
	LastClose = body['LastClose']
	High = body['High']
	Low = body['Low']
	Diff = body['Diff']
	DiffRate = body['DiffRate']
	BP1 = body['BP1']
	BV1 = body['BV1']
	SP1 = body['SP1']
	SV1 = body['SV1']
	TotalVol = body['TotalVol']
	Time = body['Time']
	LastTime = body['LastTime']
	BS = body['BS']
	Depth = body['Depth']

	#处理业务逻辑...........

	print(StockCode)

def on_error(ws, error):
	global should_reconnect,error_occurred
	if not error_occurred:  # 只在第一次错误时处理
		print("### 异常 ###")
		print(error)
		error_occurred = True  # 设置为 True 表示发生错误
		should_reconnect = True  # 设置为 True 表示需要重连

def on_close(ws):
	global should_reconnect
	print("### 连接断开 ###")
	should_reconnect = True  # 设置为 True 表示需要重连

def on_open(ws):
	global should_reconnect,error_occurred
	print("### 连接成功 ###")
	should_reconnect = False  # 连接成功，不需要重连
	error_occurred = False  # 重置错误状态

	# 建立连接后订阅品种
	data = {
		'Key': 'btcusdt'	#订阅产品
	}
	ws.send(json.dumps(data))

	# 间隔10秒发送心跳信息
	def run(*args):
		while(True) :

			global should_reconnect
			if not should_reconnect:
				break

			time.sleep(10)
			ping = {
				'ping' : int(time.time())
			}
			ws.send(json.dumps(ping))
	thread.start_new_thread(run, ())

def connect():
	websocket_url = "ws://39.107.99.235/ws"  # 替换为你的 WebSocket URL
	ws = websocket.WebSocketApp(websocket_url,
								on_data=on_data,
								on_error=on_error,
								on_close=on_close)
	ws.on_open = on_open
	return ws

def run():
	while True:
		ws = connect()
		try:
			ws.run_forever()
		except Exception as e:
			print(f"Exception occurred: {e}")

		# 当连接关闭或发生错误时，进入重连逻辑
		while should_reconnect:
			print("Waiting for 5 seconds before reconnecting...")	#间隔5秒后重新连接
			time.sleep(5)  # 等待 5 秒再重试
			ws = connect()  # 创建新连接
			try:
				ws.run_forever()
			except Exception as e:
				print(f"Reconnect exception: {e}")


if __name__ == "__main__":
	run()  # 初始连接

