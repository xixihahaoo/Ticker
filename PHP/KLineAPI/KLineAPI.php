<?php
/**
 * K线图数据接口调用类
 * 支持获取外汇K线数据
 */
class KLineAPI {
    //定义K线地址
    private $base_url = 'http://39.107.99.235:1008/redis.php';

    /**
     * 获取K线数据
     *
     * @param string $code 产品代码
     * @param string $time 时间格式 (1m, 5m, 15m, 30m, 1h, 1d, 1M)
     * @param int $rows 获取条数
     * @return array K线数据数组
     * @throws Exception
     */
    public function getKLineData($code, $time, $rows) {
        // 参数验证
        $this->validateParams($code, $time, $rows);

        // 构建请求URL
        $url = $this->buildRequestUrl($code, $time, $rows);

        // 设置请求头
        $headers = [
            'Accept-Encoding: gzip'
        ];

        // 发送HTTP请求
        $response = $this->httpGet($url, $headers);

        // 解析JSON响应
        $data = json_decode($response, true);

        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception('JSON解析错误: ' . json_last_error_msg());
        }

        return $data;
    }

    /**
     * 参数验证
     */
    private function validateParams($code, $time, $rows) {
        if (empty($code)) {
            throw new Exception('产品代码不能为空');
        }

        $validTimeframes = ['1m', '5m', '15m', '30m', '1h', '1d', '1M'];
        if (!in_array($time, $validTimeframes)) {
            throw new Exception('时间格式不支持，支持的格式: ' . implode(', ', $validTimeframes));
        }

        if ($rows <= 0) {
            throw new Exception('获取条数必须大于0');
        }
    }

    /**
     * 构建请求URL
     */
    private function buildRequestUrl($code, $time, $rows) {
        $params = [
            'code' => $code,
            'time' => $time,
            'rows' => $rows
        ];

        return $this->base_url . '?' . http_build_query($params);
    }

    /**
     * HTTP GET请求
     */
    private function httpGet($url, $headers = []) {
        $ch = curl_init();

        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => $headers,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_SSL_VERIFYHOST => false,
            CURLOPT_ENCODING => 'gzip' // 支持gzip解压缩
        ]);

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);

        curl_close($ch);

        if ($error) {
            throw new Exception('HTTP请求失败: ' . $error);
        }

        if ($httpCode !== 200) {
            throw new Exception('HTTP请求错误，状态码: ' . $httpCode);
        }

        return $response;
    }

    /**
     * 格式化K线数据（便于阅读）
     */
    public function formatKLineData($data) {
        $formatted = [];

        foreach ($data as $item) {
            $formatted[] = [
                'timestamp' => $item[0],
                'open' => $item[1],
                'high' => $item[2],
                'low' => $item[3],
                'close' => $item[4],
                'datetime' => $item[5],
                'volume' => $item[6]
            ];
        }

        return $formatted;
    }
}

// 使用示例
try {
    $api = new KLineAPI();

    // 获取英镑兑美元1分钟K线数据，最新40条
    $klineData = $api->getKLineData('fx_sgbpusd', '1m', 40);

    // 原始数据
    echo "原始K线数据:\n";
    print_r($klineData);

    // 格式化后的数据
    echo "\n格式化后的K线数据:\n";
    $formattedData = $api->formatKLineData($klineData);
    print_r($formattedData);

    // 数据统计
    echo "\n数据统计:\n";
    echo "获取到 " . count($klineData) . " 条K线数据\n";
    if (!empty($klineData)) {
        $latest = $klineData[0];
        echo "最新数据时间: {$latest[5]}\n";
        echo "开盘: {$latest[1]}, 最高: {$latest[2]}, 最低: {$latest[3]}, 收盘: {$latest[4]}\n";
    }

} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}


// 另一个使用示例 - 获取不同时间周期的数据
//function fetchMultipleTimeframes() {
//    $api = new KLineAPI();
//    $timeframes = [
//        '1m' => 10,
//        '5m' => 20,
//        '1h' => 15,
//        '1d' => 30
//    ];
//
//    $results = [];
//
//    foreach ($timeframes as $tf => $rows) {
//        try {
//            $data = $api->getKLineData('fx_sgbpusd', $tf, $rows);
//            $results[$tf] = [
//                'count' => count($data),
//                'latest_time' => !empty($data) ? $data[0][5] : '无数据'
//            ];
//        } catch (Exception $e) {
//            $results[$tf] = ['error' => $e->getMessage()];
//        }
//    }
//
//    return $results;
//}
//
//// 获取多时间框架数据
//echo "\n多时间框架数据获取:\n";
//$multiData = fetchMultipleTimeframes();
//print_r($multiData);