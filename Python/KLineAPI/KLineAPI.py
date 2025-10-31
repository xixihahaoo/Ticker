import requests
import json
import gzip
from typing import List, Dict, Any, Optional

# K线图数据接口调用类
# 支持获取外汇K线数据
class KLineAPI:
    def __init__(self):
        #定义K线地址
        self.base_url = 'http://39.107.99.235:1008/redis.php'

    def get_kline_data(self, code: str, time_frame: str, rows: int) -> List[List]:
        """
        获取K线数据

        Args:
            code: 产品代码
            time_frame: 时间格式 (1m, 5m, 15m, 30m, 1h, 1d, 1M)
            rows: 获取条数

        Returns:
            K线数据数组
        """
        # 参数验证
        self._validate_params(code, time_frame, rows)

        # 构建请求URL
        url = self._build_request_url(code, time_frame, rows)

        # 设置请求头
        headers = {
            'Accept-Encoding': 'gzip'
        }

        # 发送HTTP请求
        response = self._http_get(url, headers)

        # 解析JSON响应
        try:
            data = json.loads(response)
            return data
        except json.JSONDecodeError as e:
            raise Exception(f'JSON解析错误: {e}')

    def _validate_params(self, code: str, time_frame: str, rows: int):
        """参数验证"""
        if not code:
            raise Exception('产品代码不能为空')

        valid_timeframes = ['1m', '5m', '15m', '30m', '1h', '1d', '1M']
        if time_frame not in valid_timeframes:
            raise Exception(f'时间格式不支持，支持的格式: {", ".join(valid_timeframes)}')

        if rows <= 0:
            raise Exception('获取条数必须大于0')

    def _build_request_url(self, code: str, time_frame: str, rows: int) -> str:
        """构建请求URL"""
        params = {
            'code': code,
            'time': time_frame,
            'rows': rows
        }

        query_string = '&'.join([f'{k}={v}' for k, v in params.items()])
        return f'{self.base_url}?{query_string}'

    def _http_get(self, url: str, headers: Dict[str, str]) -> str:
        """HTTP GET请求"""
        try:
            response = requests.get(
                url,
                headers=headers,
                timeout=30,
                verify=False  # 忽略SSL证书验证
            )

            if response.status_code != 200:
                raise Exception(f'HTTP请求错误，状态码: {response.status_code}')

            # 处理gzip压缩响应
            if response.headers.get('Content-Encoding') == 'gzip':
                return gzip.decompress(response.content).decode('utf-8')
            else:
                return response.text

        except requests.exceptions.RequestException as e:
            raise Exception(f'HTTP请求失败: {e}')

    def format_kline_data(self, data: List[List]) -> List[Dict[str, Any]]:
        """
        格式化K线数据（便于阅读）

        Args:
            data: 原始K线数据

        Returns:
            格式化后的K线数据
        """
        formatted = []

        for item in data:
            formatted.append({
                'timestamp': item[0],
                'open': item[1],
                'high': item[2],
                'low': item[3],
                'close': item[4],
                'datetime': item[5],
                'volume': item[6]
            })

        return formatted


# 使用示例
if __name__ == "__main__":
    api = KLineAPI()

    try:
        # 获取英镑兑美元1分钟K线数据，最新40条
        print("获取K线数据...")
        kline_data = api.get_kline_data('fx_sgbpusd', '1m', 40)

        # 原始数据
        print("\n原始K线数据:")
        for i, item in enumerate(kline_data):
            print(f"{i+1}. {item}")

        # 格式化后的数据
        print("\n格式化后的K线数据:")
        formatted_data = api.format_kline_data(kline_data)
        for i, item in enumerate(formatted_data):
            print(f"{i+1}. {item}")

        # 数据统计
        print(f"\n数据统计:")
        print(f"获取到 {len(kline_data)} 条K线数据")
        if kline_data:
            latest = kline_data[0]
            print(f"最新数据时间: {latest[5]}")
            print(f"开盘: {latest[1]}, 最高: {latest[2]}, 最低: {latest[3]}, 收盘: {latest[4]}")

    except Exception as e:
        print(f"错误: {e}")


# 另一个使用示例 - 获取不同时间周期的数据
# def fetch_multiple_timeframes():
#     """获取多时间框架数据"""
#     api = KLineAPI()
#     timeframes = [
#         ('1m', 10),
#         ('5m', 20),
#         ('1h', 15),
#         ('1d', 30)
#     ]
#
#     results = {}
#
#     for tf, rows in timeframes:
#         try:
#             data = api.get_kline_data('fx_sgbpusd', tf, rows)
#             results[tf] = {
#                 'count': len(data),
#                 'latest_time': data[0][5] if data else '无数据'
#             }
#         except Exception as e:
#             results[tf] = {'error': str(e)}
#
#     return results
#
#
# # 测试多时间框架数据获取
# print("\n" + "="*50)
# print("多时间框架数据获取测试:")
# print("="*50)
# multi_data = fetch_multiple_timeframes()
# print("\n多时间框架结果:")
# for tf, result in multi_data.items():
#     print(f"{tf}: {result}")