<?php
/**
 * 实时行情数据获取
 */

function getRealTimeQuote($code) {
    $url = "http://39.107.99.235:1008/getQuote.php?code=" . urlencode($code);

    $options = [
        'http' => [
            'method' => 'GET',
            'header' => "Accept-Encoding: gzip\r\n",
            'timeout' => 10
        ]
    ];

    $context = stream_context_create($options);
    $response = file_get_contents($url, false, $context);

    if ($response === false) {
        return ['error' => '请求失败'];
    }

    // 处理gzip压缩
    foreach ($http_response_header as $header) {
        if (strpos(strtolower($header), 'content-encoding') !== false &&
            strpos(strtolower($header), 'gzip') !== false) {
            $response = gzdecode($response);
            break;
        }
    }

    return json_decode($response, true);
}

// 使用示例
$code = 'btcusdt';
$data = getRealTimeQuote($code);

if (isset($data['code']) && $data['code'] == 200) {
    $body = $data['data']['body'];

    echo "=== {$body['StockCode']} 实时行情 ===\n";
    echo "价格: {$body['Price']}\n";
    echo "涨跌: {$body['Diff']} ({$body['DiffRate']}%)\n";
    echo "买卖: {$body['BP1']} × {$body['BV1']} / {$body['SP1']} × {$body['SV1']}\n";
    echo "时间: {$body['Time']}\n";

} else {
    echo "获取数据失败: " . ($data['msg'] ?? '未知错误') . "\n";
}
?>