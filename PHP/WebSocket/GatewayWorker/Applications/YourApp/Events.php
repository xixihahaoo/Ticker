<?php
/**
 * This file is part of workerman.
 *
 * Licensed under The MIT License
 * For full copyright and license information, please see the MIT-LICENSE.txt
 * Redistributions of files must retain the above copyright notice.
 *
 * @author walkor<walkor@workerman.net>
 * @copyright walkor<walkor@workerman.net>
 * @link http://www.workerman.net/
 * @license http://www.opensource.org/licenses/mit-license.php MIT License
 */

/**
 * 用于检测业务代码死循环或者长时间阻塞等问题
 * 如果发现业务卡死，可以将下面declare打开（去掉//注释），并执行php start.php reload
 * 然后观察一段时间workerman.log看是否有process_timeout异常
 */
//declare(ticks=1);

use \GatewayWorker\Lib\Gateway;
use \GatewayWorker\Lib\DbConnection;
use \Workerman\Connection\AsyncTcpConnection;
use \Workerman\Lib\Timer;
use \Workerman\Worker;

/**
 * 行情对接地址：http://39.107.99.235:1008/market
 */
class Events
{
    /**
     * AsyncTcpConnection websocket异步客户端
     * 接收消息转发
     *
     * @param $businessWorker
     */
    public static function onWorkerStart($businessWorker)
    {
        if ($businessWorker->id == 0) {

            $con = new AsyncTcpConnection('ws://39.107.99.235/ws');

            $con->onConnect = function ($con)
            {
                $payload = json_encode([
                    'Key'   => "btcusdt,ethusdt",
                ]);

                $con->send($payload);

                //连接成功后，每隔10秒发送心跳
                Timer::add(10, function () use ($con)
                {
                    $con->send(json_encode([
                        'ping' => time(),
                    ]));
                });

            };

            //接收到数据
            $con->onMessage = function ($con, $msg)
            {
                $data = json_decode($msg, true);

                if (!isset($data['body']) || empty($data['body'])) {
                    return;
                }

                 $data = $data['body'];

                //处理业务逻辑..........

                print_r($data);
            };

            $con->onClose = function ($con)
            {
                //每隔10秒重新连接
                $con->reConnect(10);
            };

            $con->connect();
        }

    }

    /**
     * 当客户端连接时触发
     * 如果业务不需此回调可以删除onConnect
     * 
     * @param int $client_id 连接id
     */
    public static function onConnect($client_id)
    {
        // 向当前client_id发送数据 
        Gateway::sendToClient($client_id, "Hello $client_id\r\n");
        // 向所有人发送
        Gateway::sendToAll("$client_id login\r\n");
    }
    
   /**
    * 当客户端发来消息时触发
    * @param int $client_id 连接id
    * @param mixed $message 具体消息
    */
   public static function onMessage($client_id, $message)
   {
        // 向所有人发送 
        Gateway::sendToAll("$client_id said $message\r\n");
   }
   
   /**
    * 当用户断开连接时触发
    * @param int $client_id 连接id
    */
   public static function onClose($client_id)
   {
       // 向所有人发送 
       GateWay::sendToAll("$client_id logout\r\n");
   }
}
